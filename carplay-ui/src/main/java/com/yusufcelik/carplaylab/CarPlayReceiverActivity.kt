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

/** Foreground-only receiver experiment. Background/boot ownership is intentionally not claimed. */
open class CarPlayReceiverActivity : Activity(), SurfaceHolder.Callback, CarPlaySessionService.Observer {
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private lateinit var video: SurfaceView
    private var controller: CarPlayController? = null
    private var sink: AndroidMediaSink? = null
    @Volatile private var discovery: CarPlayDiscoveryRecords<android.bluetooth.BluetoothServerSocket>? = null
    private var discoveryConsentPending = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val discoveryExpiry = Runnable { stopDiscovery(); report("CarPlay keşif penceresi sona erdi.") }
    private val prefs by lazy { getSharedPreferences("carplay-lab", MODE_PRIVATE) }
    private var peer: String? = null
    private var hostBluetoothMac: String? = null
    private val chrome = mutableListOf<View>()
    private var fullscreen = false
    private var hadVideo = false
    private lateinit var rootLayout: LinearLayout
    private var displayProfile = CarPlayDisplayProfile.DEFAULT
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
        if (Build.VERSION.SDK_INT >= 28) window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 30)
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.decorView.systemUiVisibility = immersiveFlags()
        displayProfile = CarPlaySessionService.displayProfile() ?: run {
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            CarPlayDisplayProfile.fromBounds(metrics.widthPixels, metrics.heightPixels)
        }
        peer = intent.getStringExtra("selectedPhoneMac")?.takeIf(::validMac) ?: prefs.getString("iphone", null)
        peer?.let { prefs.edit().putString("iphone", it).apply() }
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
        rootLayout = root
        root.addView(TextView(this).apply {
            text = "Fiorino • Kablosuz CarPlay (deneysel)"
            textSize = 22f; setTextColor(Color.WHITE)
        })
        chrome += root.getChildAt(0)
        val buttons = LinearLayout(this)
        fun button(label: String, action: () -> Unit): Button = Button(this).apply {
            text = label; setOnClickListener { action() }
            buttons.addView(this, LinearLayout.LayoutParams(0, dp(56), 1f))
        }
        button("CarPlay keşif testi") { startDiscovery() }
        button("Telefon değiştir") {
            val main = if (packageName == "com.yusufcelik.fiorinoheadunit") packageManager.getLaunchIntentForPackage(packageName) else null
            if (main != null) startActivity(main.putExtra("show_driver_selector", true)) else choosePhone()
        }
        startButton = button("CarPlay başlat") { startReceiver() }
        button("Cihazları yönet") {
            val main = packageManager.getLaunchIntentForPackage(packageName)
            if (packageName == "com.yusufcelik.fiorinoheadunit" && main != null)
                startActivity(main.putExtra("manage_saved_phones", true))
            else report("Telefon eşleşmelerini Android Bluetooth ayarlarından yönetebilirsin.")
        }
        button("Alıcı adresi") { configureHostAddress() }
        button("Durdur") { stopDiscovery(); stopReceiver() }
        root.addView(buttons)
        chrome += buttons
        status = TextView(this).apply {
            setTextColor(Color.rgb(190, 207, 233)); textSize = 16f
            text = "İlk eşleştirmeyi iPhone’da onayla. Wi-Fi ve Bluetooth açık kalmalı.\nYerel aksesuar kimliği doğrulanmadan CarPlay bağlantısı başlatılmaz."
        }
        root.addView(status)
        chrome += status
        val auto = CheckBox(this).apply {
            text = "Kayıtlı iPhone’a otomatik bağlan"; setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("auto-connect", true)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("auto-connect", checked).apply() }
        }
        root.addView(auto); chrome += auto
        video = SurfaceView(this).apply {
            holder.addCallback(this@CarPlayReceiverActivity)
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
        // The negotiated canvas and touch viewport always share an aspect ratio.
        val viewport = object : android.widget.FrameLayout(this) {
            override fun onMeasure(w: Int, h: Int) {
                super.onMeasure(w, h)
                val size = CarPlayViewportPolicy.size(measuredWidth, measuredHeight, displayProfile, fullscreen)
                video.measure(View.MeasureSpec.makeMeasureSpec(size.first, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(size.second, View.MeasureSpec.EXACTLY))
            }
            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                val x = (width - video.measuredWidth) / 2
                val y = (height - video.measuredHeight) / 2
                video.layout(x, y, x + video.measuredWidth, y + video.measuredHeight)
            }
        }
        viewport.addView(video)
        root.addView(viewport, LinearLayout.LayoutParams(-1, 0, 1f))
        val frame = FrameLayout(this).apply { addView(root) }
        val menu = Button(this).apply {
            text = "☰"
            setOnClickListener { setProjectionFullscreen(!fullscreen) }
        }
        frame.addView(menu, FrameLayout.LayoutParams(dp(56), dp(44), Gravity.TOP or Gravity.END))
        setContentView(frame)
        if (intent.getBooleanExtra("startReceiver", false)) root.post {
            if (!CarPlaySessionService.isRunning()) {
                displayProfile = CarPlayDisplayProfile.fromBounds(window.decorView.width, window.decorView.height)
                prefs.edit().putInt("display-width", displayProfile.width).putInt("display-height", displayProfile.height).apply()
                video.requestLayout()
            }
            startReceiver()
        }
        if (intent.getBooleanExtra("discover", false)) root.post { startDiscovery() }
    }

    /** Tests only whether Android's SDP/EIR path makes the accessory visible in iOS CarPlay settings. */
    private fun startDiscovery() {
        if (!permissionsReady()) return
        if (controller != null || !CarPlaySessionService.isReleased()) { report("Keşif testinden önce mevcut oturumu durdur."); return }
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
        if (controller != null) { report("Telefonu değiştirmeden önce Durdur’a dokun."); return }
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
        if (controller != null) return
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
            report("Alıcı adresini ilk kurulumda bir kez kaydet.")
            configureHostAddress()
            return
        }
        prefs.edit().putString("iphone", peer).putString("host-bt-mac", bluetoothMac)
            .putBoolean("paused", false).putBoolean("auto-connect", prefs.getBoolean("auto-connect", true)).apply()
        val service = Intent(this, CarPlaySessionService::class.java).setAction(CarPlaySessionService.CONNECT)
            .putExtra("user-visible", true)
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
        } catch (_: Exception) { report("CarPlay hizmeti başlatılamadı. Arka plan izinlerini kontrol et.") }
    }

    private fun stopReceiver() {
        prefs.edit().putBoolean("paused", true).apply()
        CarPlaySessionService.stop(this)
        setProjectionFullscreen(false)
    }
    private fun configureHostAddress() {
        val input = EditText(this).apply {
            hint = "AA:BB:CC:DD:EE:FF"
            setSingleLine(true)
            setText(hostBluetoothMac ?: "")
        }
        val dialog = AlertDialog.Builder(this).setTitle("Teybin Bluetooth adresi")
            .setMessage("Adres otomatik okunamıyorsa teybin Bluetooth / cihaz bilgileri ekranındaki kendi adresini bir kez kaydet. Telefonun adresini kullanma.")
            .setView(input).setPositiveButton("Kaydet", null).setNegativeButton("Vazgeç", null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value = input.text.toString().trim().uppercase()
            if (!validMac(value)) { input.error = "Geçerli alıcı adresini gir"; return@setOnClickListener }
            hostBluetoothMac = value
            prefs.edit().putString("host-bt-mac", value).apply()
            dialog.dismiss(); report("Alıcı adresi kaydedildi. Her bağlantıda yeniden girmen gerekmez.")
        } }
        dialog.show()
    }
    private fun immersiveFlags() = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    private fun setProjectionFullscreen(enabled: Boolean) {
        fullscreen = enabled
        rootLayout.setPadding(if (enabled) 0 else dp(16), if (enabled) 0 else dp(10),
            if (enabled) 0 else dp(16), if (enabled) 0 else dp(10))
        video.requestLayout()
        chrome.forEach { it.visibility = if (enabled) View.GONE else View.VISIBLE }
        if (enabled) window.decorView.systemUiVisibility = immersiveFlags()
    }
    override fun update(message: String, controller: CarPlayController?, sink: AndroidMediaSink?, videoActive: Boolean) {
        this.controller = controller; this.sink = sink
        CarPlaySessionService.displayProfile()?.let { profile ->
            if (profile != displayProfile) { displayProfile = profile; video.requestLayout() }
        }
        startButton.isEnabled = controller == null
        if (video.holder.surface.isValid) sink?.setSurface(110, video.holder.surface)
        report(message)
        if (videoActive && !hadVideo) setProjectionFullscreen(true)
        if (!videoActive && hadVideo) setProjectionFullscreen(false)
        hadVideo = videoActive
    }
    override fun onStart() {
        super.onStart(); CarPlaySessionService.observe(this)
        if (CarPlaySessionService.isRunning()) {
            val intent = Intent(this, CarPlaySessionService::class.java).putExtra("user-visible", true)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
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
        if (isFinishing) stopDiscovery()
        CarPlaySessionService.unobserve(this)
        sink?.clearSurface(110, video.holder.surface)
        controller = null; sink = null
        super.onStop()
    }
    override fun onDestroy() { stopDiscovery(); super.onDestroy() }
}
