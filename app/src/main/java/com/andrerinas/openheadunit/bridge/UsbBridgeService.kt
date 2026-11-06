package com.andrerinas.openheadunit.bridge

import android.app.*
import android.bluetooth.*
import android.content.*
import android.hardware.usb.*
import android.net.wifi.p2p.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.*
import com.andrerinas.openheadunit.aap.protocol.proto.Wireless
import java.io.*
import java.net.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Isolated AOA peripheral <-> wireless AA relay. The head unit remains the TLS/media endpoint. */
class UsbBridgeService : Service() {
    companion object {
        private val UUID_AA = UUID.fromString("4de17a00-52cb-11e6-bdf4-0800200c9a66")
        @Volatile var status = "Köprü kapalı"; private set
        @Volatile var running = false; private set
        private const val PORT = 5288
        const val STOP = "bridge.stop"
        fun stop(context: Context) { status = "Köprü kapalı"; context.stopService(Intent(context, UsbBridgeService::class.java)) }
    }
    private val main = Handler(Looper.getMainLooper())
    private lateinit var wifi: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private var session: Session? = null
    private var generation = 0
    private var registered = false
    private val detach = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { if (i.action == UsbManager.ACTION_USB_ACCESSORY_DETACHED) stopSelf() }
    }
    override fun onBind(i: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        running = true
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("usb-bridge", "USB kablosuz köprü", NotificationManager.IMPORTANCE_LOW))
        update("USB köprüsü hazırlanıyor")
        wifi = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifi.initialize(this, mainLooper, null)
        ContextCompat.registerReceiver(this, detach, IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED), ContextCompat.RECEIVER_EXPORTED)
        registered = true
    }
    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        if (i?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        val target = i?.getStringExtra("phone")
        if (!BridgeMode.enabled(this) || target == null || !BluetoothAdapter.checkBluetoothAddress(target)) {
            stopSelf(); return START_NOT_STICKY
        }
        val usb = getSystemService(Context.USB_SERVICE) as UsbManager
        val accessory = usb.accessoryList?.firstOrNull { BridgeMode.accessorySupported(it.manufacturer, it.model) }
        if (accessory == null || !usb.hasPermission(accessory)) {
            update("Aracın USB Android Auto bağlantısı ve USB izni gerekli")
            stopSelf(); return START_NOT_STICKY
        }
        val token = ++generation
        val previous = session
        previous?.close(); session = null
        if (previous != null) {
            Thread {
                val released = previous.awaitRetired()
                main.post {
                    if (token != generation || !running) return@post
                    if (released) resetNetwork(token, target, accessory)
                    else update("Önceki USB işlemi kapanmadı. Araç USB kablosunu çıkarıp tekrar tak.")
                }
            }.start()
        } else resetNetwork(token, target, accessory)
        return START_NOT_STICKY
    }
    private fun resetNetwork(token: Int, target: String, accessory: UsbAccessory) {
        // New credentials stop the previous driver's cached network from winning the next race.
        wifi.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = createNetwork(token, target, accessory)
            override fun onFailure(reason: Int) = createNetwork(token, target, accessory)
        })
    }
    private fun createNetwork(token: Int, target: String, accessory: UsbAccessory) {
        if (token != generation || !running) return
        update("Seçilen telefon için yeni kablosuz ağ kuruluyor")
        val listener = object : WifiP2pManager.ActionListener {
            override fun onFailure(reason: Int) { if (token == generation) { update("Wi-Fi Direct ağı kurulamadı ($reason). Wi-Fi ve konum izinlerini kontrol et."); stopSelf() } }
            override fun onSuccess() { pollGroup(token, target, accessory, 0) }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val random = java.security.SecureRandom()
            val secret = ByteArray(18).also(random::nextBytes).joinToString("") { "%02x".format(it.toInt() and 255) }
            val config = WifiP2pConfig.Builder().setNetworkName("DIRECT-FI-Fiorino-${random.nextInt(10000)}")
                .setPassphrase(secret).build()
            wifi.createGroup(channel, config, listener)
        } else wifi.createGroup(channel, listener)
    }
    private fun pollGroup(token: Int, target: String, accessory: UsbAccessory, tries: Int) {
        if (token != generation || !running) return
        wifi.requestGroupInfo(channel) { group ->
            if (token != generation || !running) return@requestGroupInfo
            if (group == null || group.networkName.isNullOrBlank() || group.passphrase.isNullOrBlank()) {
                if (tries < 30) main.postDelayed({ pollGroup(token, target, accessory, tries + 1) }, 200)
                else { update("Kablosuz ağ bilgileri alınamadı"); stopSelf() }
            } else {
                wifi.requestConnectionInfo(channel) { info ->
                    if (token != generation || !running) return@requestConnectionInfo
                    if (!info.isGroupOwner || info.groupOwnerAddress == null) { update("Telefon erişim noktası olamadı"); stopSelf(); return@requestConnectionInfo }
                    val s = Session(target, accessory, group, info.groupOwnerAddress.hostAddress!!, token)
                    session = s
                    s.launch()
                }
            }
        }
    }
    private fun update(text: String) {
        status = text
        val open = PendingIntent.getActivity(this, 150, Intent(this, UsbBridgeActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 151, Intent(this, UsbBridgeService::class.java).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "usb-bridge").setSmallIcon(R.drawable.ic_stat_aa)
            .setContentTitle("Fiorino • Android Auto köprüsü").setContentText(text).setContentIntent(open)
            .setOngoing(true).addAction(0, "Telefon değiştir", open).addAction(0, "Durdur", stop).build()
        startForeground(150, n)
    }
    override fun onDestroy() {
        running = false; generation++
        main.removeCallbacksAndMessages(null)
        session?.close(); session = null
        if (::wifi.isInitialized && ::channel.isInitialized) {
            wifi.removeGroup(channel, null)
            if (Build.VERSION.SDK_INT >= 27) channel.close()
        }
        if (registered) unregisterReceiver(detach)
        super.onDestroy()
    }
    private inner class Session(val target: String, val accessory: UsbAccessory, val group: WifiP2pGroup, val ip: String, val token: Int) {
        private val closed = AtomicBoolean(false)
        private val btBusy = AtomicBoolean(false)
        private val resources = ConcurrentHashMap.newKeySet<Closeable>()
        @Volatile private var connected = false
        @Volatile private var reader: Thread? = null
        private val worker = Thread({ run() }, "AA-USB-bridge")
        fun launch() = worker.start()
        fun awaitRetired(): Boolean {
            worker.join(2000)
            reader?.join(1500)
            return !worker.isAlive && reader?.isAlive != true
        }
        private fun <T : Closeable> own(r: T): T {
            resources.add(r)
            if (closed.get()) { r.close(); throw IOException("Retired bridge") }
            return r
        }
        private fun report(s: String) { main.post { if (token == generation && running) update(s) } }
        fun run() {
            try {
                val adapter = getSystemService(BluetoothManager::class.java).adapter ?: throw IOException("Bluetooth unavailable")
                val usb = getSystemService(Context.USB_SERVICE) as UsbManager
                val fd = own(usb.openAccessory(accessory) ?: throw IOException("USB could not open"))
                val usbIn = FileInputStream(fd.fileDescriptor)
                val usbOut = FileOutputStream(fd.fileDescriptor)
                val tcp = own(ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(ip, PORT)) })
                tcp.soTimeout = 90_000
                val listener = own(adapter.listenUsingRfcommWithServiceRecord("Fiorino USB Bridge", UUID_AA))
                Thread({
                    try { while (!closed.get()) {
                        val incoming = own(listener.accept())
                        if (!incoming.remoteDevice.address.equals(target, true)) { incoming.close(); resources.remove(incoming) }
                        else handshake(incoming)
                    } } catch (_: Exception) { /* closed by owner */ }
                }, "Bridge-BT-listener").start()
                Thread({
                    while (!closed.get() && !connected) {
                        try {
                            adapter.cancelDiscovery()
                            val socket = own(adapter.getRemoteDevice(target).createRfcommSocketToServiceRecord(UUID_AA))
                            // Closing the session owns cancellation even if OEM connect() blocks.
                            val timeout = Thread { try { Thread.sleep(8000); if (!socket.isConnected) socket.close() } catch (_: Exception) {} }
                            timeout.isDaemon = true; timeout.start()
                            socket.connect(); handshake(socket)
                        } catch (_: Exception) { /* paired phone may initiate toward our listener instead */ }
                        try { Thread.sleep(10_000) } catch (_: InterruptedException) { break }
                    }
                }, "Bridge-BT-wake").start()
                report("Telefon bekleniyor • araç USB üzerinden bağlı")
                val phone = own(tcp.accept()).apply { tcpNoDelay = true; soTimeout = 30_000 }
                connected = true
                report("Veri köprüsü açık • görüntü aracın Android Auto ekranında olmalı")
                val up = Thread({
                    try {
                        val buffer = ByteArray(65536)
                        while (!closed.get()) {
                            val poll = android.system.StructPollfd().apply {
                                this.fd = fd.fileDescriptor
                                events = android.system.OsConstants.POLLIN.toShort()
                            }
                            if (android.system.Os.poll(arrayOf(poll), 500) == 0) continue
                            if (closed.get()) break
                            val count = usbIn.read(buffer)
                            if (count <= 0) break
                            if (count > 0) phone.getOutputStream().write(buffer, 0, count)
                        }
                    } catch (_: Exception) {} finally { close() }
                }, "Bridge-USB-to-WiFi")
                reader = up
                up.start()
                try { while (!closed.get() && BridgeFrames.forwardFrame(phone.getInputStream(), usbOut)) {} }
                finally { close(); up.join(1000) }
                main.post { if (token == generation && running) update("Oturum bitti. Yeni oturum için araç USB kablosunu çıkarıp tak; ardından telefonu seç.") }
            } catch (_: Exception) {
                report("Köprü bağlantısı kurulamadı. USB kablosunu yeniden takıp Bluetooth/Wi-Fi izinlerini kontrol et.")
            } finally { close() }
        }
        private fun handshake(socket: BluetoothSocket) {
            if (!btBusy.compareAndSet(false, true)) { socket.close(); resources.remove(socket); return }
            try {
                val output = socket.outputStream
                fun send(type: Int, body: ByteArray) { synchronized(output) { output.write(WppFraming.encodeFrame(body, type)) } }
                // Legacy start-first WPP is supported by native AA and avoids advertising an unserved TCP WPP endpoint.
                send(WppMessageType.START_REQUEST, WppMessages.startRequest(ip, PORT).toByteArray())
                val input = DataInputStream(socket.inputStream)
                while (!closed.get()) {
                    val size = input.readUnsignedShort(); val type = input.readUnsignedShort()
                    val body = ByteArray(size); input.readFully(body)
                    when (type) {
                        WppMessageType.INFO_REQUEST -> send(WppMessageType.INFO_RESPONSE, Wireless.WifiInfoResponse.newBuilder()
                            .setSsid(group.networkName).setKey(group.passphrase).setBssid(group.owner?.deviceAddress.orEmpty())
                            .setSecurityMode(Wireless.SecurityMode.WPA2_PERSONAL).setAccessPointType(Wireless.AccessPointType.DYNAMIC).build().toByteArray())
                        WppMessageType.PING_REQUEST -> send(WppMessageType.PING_RESPONSE, body)
                    }
                }
            } catch (_: Exception) {} finally { socket.close(); resources.remove(socket); btBusy.set(false) }
        }
        fun close() { if (closed.compareAndSet(false, true)) resources.forEach { runCatching { it.close() } } }
    }
}
