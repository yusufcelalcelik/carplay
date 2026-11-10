package com.andrerinas.openheadunit.utils

import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Read-only log page for diagnosing a head unit that has no adb. Off until switched on in settings,
 * PIN protected, stops by itself. It only ever serves [LogRing]; it runs no command and changes nothing.
 */
object LogViewerServer {
    const val PORT = 8765
    const val AUTO_STOP_MS = 30 * 60_000L
    private const val MAX_HEADER_BYTES = 8192
    private const val READ_DEADLINE_MS = 5000L

    @Volatile var pin: String? = null
        private set
    @Volatile private var server: ServerSocket? = null
    @Volatile private var auth: LogViewerAuth? = null
    private val pool by lazy {
        ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(8)) { r ->
            Thread(r, "LogViewer").apply { isDaemon = true }
        }
    }

    val running: Boolean get() = server != null
    val boundPort: Int get() = server?.localPort ?: PORT

    @Synchronized
    fun start(port: Int = PORT): Boolean {
        if (server != null) return true
        val socket = try {
            ServerSocket().apply { reuseAddress = true; soTimeout = 1000; bind(InetSocketAddress(port)) }
        } catch (e: Exception) {
            AppLog.w("LogViewer: could not open port $port: ${e.message}")
            return false
        }
        val code = String.format(java.util.Locale.ROOT, "%06d", SecureRandom().nextInt(1_000_000))
        pin = code
        auth = LogViewerAuth(code)
        LogRing.acquire()
        server = socket
        val deadline = System.nanoTime() + AUTO_STOP_MS * 1_000_000L
        Thread({ acceptLoop(socket, deadline) }, "LogViewer-accept").apply { isDaemon = true }.start()
        AppLog.i("LogViewer: started on port ${socket.localPort}")
        return true
    }

    @Synchronized
    fun stop() {
        val s = server ?: return
        server = null
        pin = null
        auth = null
        LogRing.release()
        try { s.close() } catch (_: Exception) {}
    }

    /** Addresses to type into the browser, one per active IPv4 interface. */
    fun addresses(): List<String> = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .map { "http://${it.hostAddress}:$boundPort" }
    } catch (_: Exception) { emptyList() }

    private fun acceptLoop(socket: ServerSocket, deadline: Long) {
        try {
            while (server === socket && System.nanoTime() - deadline < 0) {
                val client = try { socket.accept() } catch (_: java.net.SocketTimeoutException) { continue } catch (_: Exception) { break }
                try { pool.execute { handle(client) } } catch (_: Exception) { try { client.close() } catch (_: Exception) {} }
            }
        } finally {
            if (server === socket) stop()
        }
    }

    /**
     * Reads the request line and headers, at most [MAX_HEADER_BYTES] counted as they arrive and at
     * most [READ_DEADLINE_MS] in all. Returns the lines, or null when a limit was hit.
     */
    private fun readHead(c: Socket): List<String>? {
        val input = c.getInputStream()
        val end = System.nanoTime() + READ_DEADLINE_MS * 1_000_000L
        val head = java.io.ByteArrayOutputStream()
        var tail = 0
        while (true) {
            val remainingMs = (end - System.nanoTime()) / 1_000_000L
            if (remainingMs <= 0) return null
            c.soTimeout = remainingMs.toInt().coerceAtLeast(1)
            val b = input.read()
            if (b < 0) break
            if (head.size() >= MAX_HEADER_BYTES) return null
            head.write(b)
            if (b == '\n'.code) {
                // Empty line: a lone \n or \r\n right after the previous line ends the head.
                tail = if (tail == 1) 2 else 1
                if (tail == 2) break
            } else if (b != '\r'.code) tail = 0
        }
        return String(head.toByteArray(), Charsets.ISO_8859_1).split('\n').map { it.trimEnd('\r') }
    }

    private fun handle(client: Socket) = client.use { c ->
        try {
            val lines = try { readHead(c) } catch (_: java.net.SocketTimeoutException) { return }
            if (lines == null) return respond(c, 431, "text/plain", "Header too large")
            val request = lines.firstOrNull()?.takeIf { it.isNotEmpty() } ?: return
            var pinHeader: String? = null
            for (line in lines.drop(1)) {
                if (line.isEmpty()) break
                if (line.startsWith("X-Pin:", ignoreCase = true)) pinHeader = line.substring(6).trim()
            }
            val parts = request.split(' ')
            if (parts.size < 2 || parts[0] != "GET") return respond(c, 405, "text/plain", "Only GET")
            val path = parts[1].substringBefore('?')
            val query = parts[1].substringAfter('?', "")
            if (path == "/") return respond(c, 200, "text/html; charset=utf-8", PAGE)
            val attempt = pinHeader ?: query.split('&').firstOrNull { it.startsWith("pin=") }?.substring(4)
            if (auth?.check(attempt, System.nanoTime() / 1_000_000) != true) return respond(c, 403, "text/plain", "PIN")
            when (path) {
                "/log" -> {
                    val after = query.split('&').firstOrNull { it.startsWith("since=") }?.substring(6)?.toLongOrNull() ?: 0L
                    val (seq, lines) = LogRing.since(after)
                    respond(c, 200, "text/plain; charset=utf-8", lines.joinToString("\n"), "X-Seq: $seq\r\n")
                }
                "/download" -> respond(c, 200, "text/plain; charset=utf-8", LogRing.since(0).second.joinToString("\n"),
                    "Content-Disposition: attachment; filename=\"fiorino-log.txt\"\r\n")
                else -> respond(c, 404, "text/plain", "Not found")
            }
        } catch (_: Exception) {
        }
    }

    private fun respond(c: Socket, code: Int, type: String, body: String, extra: String = "") {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val head = "HTTP/1.1 $code OK\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\n" +
            "Cache-Control: no-store\r\nAccess-Control-Expose-Headers: X-Seq\r\n${extra}Connection: close\r\n\r\n"
        c.getOutputStream().apply { write(head.toByteArray(Charsets.ISO_8859_1)); write(bytes); flush() }
    }

    private const val PAGE = """<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width">
<title>Fiorino log</title><body style="margin:0;background:#111;color:#ddd;font:12px monospace">
<div style="padding:6px;background:#222;position:sticky;top:0">PIN <input id=p size=7> <button onclick="go()">Başlat</button>
<a id=d style="color:#8cf;margin-left:12px">indir</a> <span id=s></span></div><pre id=o style="margin:6px;white-space:pre-wrap"></pre>
<script>
var seq=0,pin=location.hash.slice(1),o=document.getElementById('o'),s=document.getElementById('s'),t;
document.getElementById('p').value=pin;
function go(){pin=document.getElementById('p').value;seq=0;o.textContent='';clearTimeout(t);poll();
document.getElementById('d').href='/download?pin='+encodeURIComponent(pin);}
function poll(){fetch('/log?since='+seq,{headers:{'X-Pin':pin}}).then(function(r){
if(r.status==403){s.textContent='PIN yanlış veya kilitli';return null}
s.textContent='';var n=r.headers.get('X-Seq');return r.text().then(function(x){if(x){var b=window.innerHeight+scrollY>=document.body.scrollHeight-20;
o.textContent+=x+'\n';if(b)scrollTo(0,document.body.scrollHeight)}seq=parseInt(n)||seq;t=setTimeout(poll,1000)})
}).catch(function(){s.textContent='bağlantı yok';t=setTimeout(poll,3000)})}
if(pin)go();
</script>"""
}
