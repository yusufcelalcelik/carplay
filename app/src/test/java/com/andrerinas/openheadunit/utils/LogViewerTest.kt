package com.andrerinas.openheadunit.utils

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class LogViewerTest {
    @After fun reset() { while (LogRing.enabled) LogRing.release() }

    @Test fun `ring ignores lines while off`() {
        LogRing.offer(4, "x")
        assertTrue(LogRing.since(0).second.isEmpty())
    }

    @Test fun `ring returns only lines after the sequence`() {
        LogRing.acquire()
        LogRing.offer(4, "a"); LogRing.offer(5, "b"); LogRing.offer(6, "c")
        val (seq, all) = LogRing.since(0)
        assertEquals(3L, seq); assertEquals(3, all.size)
        val (_, tail) = LogRing.since(2)
        assertEquals(1, tail.size); assertTrue(tail[0].endsWith("E c"))
        assertTrue(LogRing.since(3).second.isEmpty())
    }

    @Test fun `ring is bounded and keeps the newest`() {
        LogRing.acquire()
        repeat(LogRing.CAPACITY + 10) { LogRing.offer(4, "l$it") }
        val (seq, lines) = LogRing.since(0)
        assertEquals(LogRing.CAPACITY, lines.size)
        assertEquals((LogRing.CAPACITY + 10).toLong(), seq)
        assertTrue(lines.last().endsWith("l${LogRing.CAPACITY + 9}"))
    }

    @Test fun `wrong pin locks out even the right pin`() {
        val auth = LogViewerAuth("123456")
        repeat(LogViewerAuth.MAX_FAILURES) { assertFalse(auth.check("000000", 1000L)) }
        assertFalse(auth.check("123456", 2000L))
        assertTrue(auth.check("123456", 1000L + LogViewerAuth.LOCKOUT_MS + 1))
    }

    @Test fun `right pin passes and null fails`() {
        val auth = LogViewerAuth("123456")
        assertTrue(auth.check("123456", 0L))
        assertFalse(auth.check(null, 0L))
    }

    private fun get(path: String, pin: String? = null): String =
        java.net.Socket("127.0.0.1", LogViewerServer.boundPort).use { c ->
            c.getOutputStream().write("GET $path HTTP/1.1\r\nHost: x\r\n${if (pin != null) "X-Pin: $pin\r\n" else ""}\r\n".toByteArray())
            c.getInputStream().readBytes().toString(Charsets.UTF_8)
        }

    @Test fun `server serves the page freely but the log only with the pin`() {
        assertTrue(LogViewerServer.start(0))
        try {
            val root = get("/"); assertTrue(root.take(300), root.startsWith("HTTP/1.1 200"))
            assertTrue(get("/log?since=0").startsWith("HTTP/1.1 403"))
            LogRing.offer(4, "hello-px3")
            val ok = get("/log?since=0", LogViewerServer.pin)
            assertTrue(ok.startsWith("HTTP/1.1 200")); assertTrue(ok.contains("hello-px3")); assertTrue(ok.contains("X-Seq: "))
            assertTrue(get("/shell", LogViewerServer.pin).startsWith("HTTP/1.1 404"))
        } finally { LogViewerServer.stop() }
        assertFalse(LogViewerServer.running)
    }

    @Test fun `an oversized header line gets no log and the server keeps serving`() {
        assertTrue(LogViewerServer.start(0))
        try {
            LogRing.offer(4, "secret-line")
            val big = "A".repeat(20_000)
            val reply = java.net.Socket("127.0.0.1", LogViewerServer.boundPort).use { c ->
                c.soTimeout = 8000
                try {
                    c.getOutputStream().write("GET /log?since=0 HTTP/1.1\r\nX-Pin: ${LogViewerServer.pin}\r\nX-Big: $big\r\n\r\n".toByteArray())
                } catch (_: java.io.IOException) {}
                try { c.getInputStream().readBytes().toString(Charsets.UTF_8) } catch (_: java.io.IOException) { "" }
            }
            assertFalse(reply.contains("secret-line"))
            assertFalse(reply.startsWith("HTTP/1.1 200"))
            assertTrue(get("/").startsWith("HTTP/1.1 200"))
        } finally { LogViewerServer.stop() }
    }
}
