package com.yusufcelik.carplaylab

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.*
import com.shilapi.xcertplay.airplay.*
import com.shilapi.xcertplay.media.AndroidMediaSink
import com.shilapi.xcertplay.mfi.LocalMfiAuthenticationClient
import com.shilapi.xcertplay.orchestration.*
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Foreground-only receiver experiment. Background/boot ownership is intentionally not claimed. */
class CarPlayLabActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private lateinit var video: SurfaceView
    private var controller: CarPlayController? = null
    private var sink: AndroidMediaSink? = null
    @Volatile private var discovery: CarPlayDiscoveryRecords<android.bluetooth.BluetoothServerSocket>? = null
    private var discoveryConsentPending = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val discoveryExpiry = Runnable { stopDiscovery(); report("CarPlay keşif penceresi sona erdi.") }
    private val closing = AtomicBoolean(false)
    private val prefs by lazy { getSharedPreferences("carplay-lab", MODE_PRIVATE) }
    private var peer: String? = null
    private var hostBluetoothMac: String? = null
    private val identity by lazy { loadIdentity() }
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        peer = prefs.getString("iphone", null)
        hostBluetoothMac = intent.getStringExtra("hostBluetoothMac")?.takeIf(::validMac)
            ?: prefs.getString("host-bt-mac", null)?.takeIf(::validMac)
            ?: runCatching { android.provider.Settings.Secure.getString(contentResolver, "bluetooth_address") }
                .getOrNull()?.takeIf(::validMac)
            ?: runCatching { adapter?.address }.getOrNull()?.takeIf(::validMac)
        hostBluetoothMac?.let { prefs.edit().putString("host-bt-mac", it).apply() }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setBackgroundColor(Color.rgb(12, 18, 31))
        }
        root.addView(TextView(this).apply {
            text = "Fiorino • Kablosuz CarPlay (deneysel)"
            textSize = 22f; setTextColor(Color.WHITE)
        })
        val buttons = LinearLayout(this)
        fun button(label: String, action: () -> Unit): Button = Button(this).apply {
            text = label; setOnClickListener { action() }
            buttons.addView(this, LinearLayout.LayoutParams(0, dp(56), 1f))
        }
        button("CarPlay keşif testi") { startDiscovery() }
        button("Kayıtlı iPhone seç") { choosePhone() }
        startButton = button("CarPlay başlat") { startReceiver() }
        button("Durdur") { stopDiscovery(); stopReceiver() }
        root.addView(buttons)
        status = TextView(this).apply {
            setTextColor(Color.rgb(190, 207, 233)); textSize = 16f
            text = "İlk eşleştirmeyi iPhone’da onayla. Wi-Fi ve Bluetooth açık kalmalı.\nYerel aksesuar kimliği doğrulanmadan CarPlay bağlantısı başlatılmaz."
        }
        root.addView(status)
        video = SurfaceView(this).apply {
            holder.addCallback(this@CarPlayLabActivity)
            setOnTouchListener { _, e ->
                val active = controller ?: return@setOnTouchListener false
                val up = e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL
                val contacts = if (up) emptyList() else (0 until minOf(2, e.pointerCount))
                    .filter { !(e.actionMasked == MotionEvent.ACTION_POINTER_UP && it == e.actionIndex) }
                    .map { AirPlayContact(e.getPointerId(it), e.getX(it).toDouble() / width,
                        e.getY(it).toDouble() / height, true) }
                active.sendTouch(contacts)
                true
            }
        }
        // Surface and touch coordinates share an 800:480 viewport; never stretch the touch map.
        val viewport = object : android.widget.FrameLayout(this) {
            override fun onMeasure(w: Int, h: Int) {
                super.onMeasure(w, h)
                val width = minOf(measuredWidth, measuredHeight * 800 / 480)
                video.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(width * 480 / 800, View.MeasureSpec.EXACTLY))
            }
            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                val x = (width - video.measuredWidth) / 2
                val y = (height - video.measuredHeight) / 2
                video.layout(x, y, x + video.measuredWidth, y + video.measuredHeight)
            }
        }
        viewport.addView(video)
        root.addView(viewport, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    /** Tests only whether Android's SDP/EIR path makes the accessory visible in iOS CarPlay settings. */
    private fun startDiscovery() {
        if (!permissionsReady()) return
        if (controller != null || closing.get()) { report("Keşif testinden önce mevcut oturumu durdur."); return }
        stopDiscovery()
        try {
            val radio = adapter ?: error("Bluetooth adapter unavailable")
            check(radio.isEnabled)
            val group = CarPlayDiscoveryRecords.open { name, uuid -> radio.listenUsingRfcommWithServiceRecord(name, uuid) }
            discovery = group
            // No iAP2/MFi responses are fabricated by the discovery experiment.
            group.records.forEach { server ->
                Thread {
                    try {
                        while (discovery === group) {
                            server.accept().use { }
                            runOnUiThread {
                                if (discovery === group) report("Bir Bluetooth bağlantı isteği alındı. Keşif testi yansıtma başlatmadı; aksesuar doğrulaması hâlâ gerekli.")
                            }
                        }
                    } catch (_: Exception) { /* Closing the bounded window unblocks accept(). */ }
                }.apply { isDaemon = true; start() }
            }
            handler.postDelayed(discoveryExpiry, 180_000)
            discoveryConsentPending = true
            report("Keşif servis kayıtları açıldı. Görünürlüğü onayladıktan sonra iPhone’da Genel → CarPlay → Kullanılabilir Araçlar ekranından Note 9’u ara. Listeleme henüz doğrulanmadı; bu test görüntü/ses oturumu açmaz.")
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180), 42)
        } catch (_: Exception) {
            discoveryConsentPending = false
            stopDiscovery()
            report("CarPlay keşif kaydı açılamadı. Bluetooth izinlerini ve Android servis kayıt desteğini kontrol et.")
        }
    }
    private fun stopDiscovery() {
        handler.removeCallbacks(discoveryExpiry)
        val old = discovery
        discovery = null
        old?.close()
    }
    @Deprecated("Platform activity result used by this foreground-only laboratory")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 42) return
        discoveryConsentPending = false
        val visible = runCatching { adapter?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE }.getOrDefault(false)
        if (resultCode <= 0 && !visible) {
            stopDiscovery()
            report("Görünürlük onaylanmadı; keşif testi durduruldu.")
        } else if (discovery != null) {
            handler.removeCallbacks(discoveryExpiry)
            handler.postDelayed(discoveryExpiry, 180_000)
        }
    }

    private fun permissionsReady(): Boolean {
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31) needed += listOf(Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE)
        if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.NEARBY_WIFI_DEVICES
        val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) return true
        requestPermissions(missing.toTypedArray(), 41)
        report("İzinlerden sonra işlemi tekrar seç.")
        return false
    }

    private fun choosePhone() {
        if (!permissionsReady()) return
        val devices = adapter?.bondedDevices.orEmpty().sortedBy { it.name ?: "" }
        if (devices.isEmpty()) { report("Önce iPhone’dan Bluetooth eşleştirmesini onayla."); return }
        AlertDialog.Builder(this).setTitle("CarPlay iPhone’u")
            .setItems(devices.map { it.name ?: "Adsız cihaz" }.toTypedArray()) { _, i ->
                peer = devices[i].address
                prefs.edit().putString("iphone", peer).apply()
                report("${devices[i].name ?: "iPhone"} kaydedildi. Seçim sonraki başlatmada kullanılır.")
            }.show()
    }

    private fun startReceiver() {
        if (!permissionsReady()) return
        stopDiscovery()
        if (controller != null || closing.get()) return
        if (peer == null) { choosePhone(); return }
        val directory = File(noBackupFilesDir, LocalMfiAuthenticationClient.DIRECTORY)
        try {
            directory.mkdirs()
            // An optional explicitly supplied local build input, never downloaded at runtime.
            for (name in listOf("identity.pk8", "certificate.p7b")) {
                val target = File(directory, name)
                if (!target.exists()) {
                    assets.open("offline-mfi/$name").use { input ->
                        val bytes = input.readBytesBounded()
                        target.writeBytes(bytes)
                        bytes.fill(0)
                    }
                }
            }
            LocalMfiAuthenticationClient.load(directory) // checks structure and key/certificate consistency
        } catch (_: Exception) {
            report("Yerel aksesuar kimliği yok veya geçersiz. Kimlik doğrulanmadı; iPhone’a CarPlay hazır diye bildirilmedi. Kurulum notlarına bak.")
            return
        }
        val bluetoothMac = hostBluetoothMac ?: run {
            report("Gerçek alıcı Bluetooth adresi okunamadı. Yerel ADB kurulum adımını tamamla; sahte adresle eşleştirme denenmedi.")
            return
        }
        try {
            val id = identity.publicKey.take(6).map { "%02X".format(it.toInt() and 255) }.joinToString(":")
            val media = AndroidMediaSink(surface = video.holder.surface.takeIf { it.isValid },
                videoWidth = 800, videoHeight = 480, context = this,
                mainVideoFrameRate = 30, audioFocusEnabled = true, videoPacingDelayMillis = 0)
            sink = media
            val pairings = PairingStore { identifier, key ->
                prefs.edit().putString("pair.$identifier", Base64.encodeToString(key, Base64.NO_WRAP)).apply()
            }
            prefs.all.filterKeys { it.startsWith("pair.") }.forEach { (k,v) ->
                if (v is String) pairings.save(k.removePrefix("pair."), Base64.decode(v, Base64.NO_WRAP))
            }
            val config = CarPlayRuntimeConfig(mfiTarget = MfiTarget.LOCAL,
                identification = Iap2IdentificationConfig("Fiorino Auto", "Fiorino-CarPlay-Lab", "Fiorino",
                    identity.pairingId, "0.1.0", "1", 0, language = "tr"),
                label = "Fiorino Auto", hostName = "fiorino-carplay",
                transport = CarPlayTransport.WIRELESS, wirelessBluetoothDeviceAddress = peer,
                wifiP2pPreferredChannel = 6)
            controller = CarPlayController(this, config,
                AirPlayConfig("Fiorino Auto", id, bluetoothMac, "220.68",
                    AirPlayDisplayConfig(800, 480, widthPhysicalMm = 250, heightPhysicalMm = 150, fps = 30),
                    manufacturer = "Fiorino", model = "Fiorino-CarPlay-Lab", oemLabel = "Fiorino Auto", microphone = true),
                identity, pairings, object : AirPlaySessionListener {
                    override val wantsTrace = false
                    override fun onSessionActive(session: AirPlaySession) { report("CarPlay kontrol oturumu kuruldu; görüntü bekleniyor.") }
                    override fun onVideoFrameRendered(session: AirPlaySession) { report("CarPlay görüntü akışı başladı.") }
                    override fun onSessionEnded(session: AirPlaySession) { report("CarPlay oturumu sona erdi.") }
                    override fun onTransportError(message: String) { report("Bağlantı hatası. Yerel logu kontrol et.") }
                }, CarPlayMediaEngine(media, microphoneEnabled = true),
                reportStatus = { s -> report(statusLabel(s)) })
            startButton.isEnabled = false
            controller?.start()
        } catch (e: Exception) {
            report("CarPlay başlatılamadı: ${e.javaClass.simpleName}")
            stopReceiver()
        }
    }

    private fun stopReceiver() {
        val old = controller; val oldSink = sink
        controller = null; sink = null
        if (old == null && oldSink == null) return
        closing.set(true)
        Thread {
            try { old?.close() } finally {
                try { oldSink?.close() } finally {
                    closing.set(false)
                    runOnUiThread { if (!isDestroyed) { startButton.isEnabled = true; report("Alıcı durduruldu.") } }
                }
            }
        }.start()
    }

    private fun loadIdentity(): AirPlayIdentity {
        val file = File(noBackupFilesDir, "airplay-identity.bin")
        if (file.exists()) return file.inputStream().buffered().use { input ->
            java.io.DataInputStream(input).let { d ->
                val privateKey = ByteArray(32).also(d::readFully)
                val publicKey = ByteArray(32).also(d::readFully)
                AirPlayIdentity(privateKey, publicKey, d.readUTF())
            }
        }
        return AirPlayIdentity.generate().also { identity ->
            val atomic = android.util.AtomicFile(file)
            val stream = atomic.startWrite()
            try {
                val d = java.io.DataOutputStream(stream)
                d.write(identity.privateKey); d.write(identity.publicKey); d.writeUTF(identity.pairingId); d.flush()
                atomic.finishWrite(stream)
            } catch (e: Exception) { atomic.failWrite(stream); throw e }
        }
    }
    private fun java.io.InputStream.readBytesBounded(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.size() + count <= 16384)
            output.write(buffer, 0, count)
        }
        return output.toByteArray().also { require(it.isNotEmpty()) }
    }
    private fun statusLabel(s: CarPlayStatus): String = when (s) {
        CarPlayStatus.DiscoveringMfi -> "Yerel aksesuar kimliği kontrol ediliyor…"
        CarPlayStatus.MfiReady -> "Yerel imzalama hazır. iPhone kabulü henüz doğrulanmadı."
        CarPlayStatus.StartingHotspot -> "CarPlay Wi-Fi ağı hazırlanıyor…"
        is CarPlayStatus.HotspotReady -> "Wi-Fi hazır: ${s.band}, kanal ${s.channel}"
        CarPlayStatus.WaitingForPairedIphone -> "Eşleşmiş iPhone bekleniyor…"
        CarPlayStatus.ConnectingBluetooth -> "iPhone ile Bluetooth el sıkışması…"
        CarPlayStatus.Pairing -> "iPhone eşleştirmesi…"
        is CarPlayStatus.Failed -> "CarPlay bağlantısı başarısız. Yerel tanılama kaydını kontrol et."
        else -> "CarPlay: ${s.javaClass.simpleName}"
    }
    private fun report(message: String) {
        Log.i("FiorinoCarPlayLab", message)
        runOnUiThread { if (!isDestroyed) status.text = message }
    }
    private fun validMac(value: String): Boolean =
        Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}").matches(value) &&
            value.uppercase() !in setOf("02:00:00:00:00:00", "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF") &&
            value.substring(0, 2).toInt(16) and 1 == 0
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun surfaceCreated(holder: SurfaceHolder) { sink?.setSurface(110, holder.surface) }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { sink?.setSurface(110, holder.surface) }
    override fun surfaceDestroyed(holder: SurfaceHolder) { sink?.clearSurface(110, holder.surface) }
    override fun onStop() {
        // Android may cover us with its secure pairing dialog. Keep only the bounded
        // discovery lease across that dialog; projection itself remains foreground-only.
        if (isFinishing) stopDiscovery()
        stopReceiver()
        super.onStop()
    }
    override fun onDestroy() { stopDiscovery(); super.onDestroy() }
}
