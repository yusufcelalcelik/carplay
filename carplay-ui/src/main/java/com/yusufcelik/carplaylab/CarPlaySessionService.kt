package com.yusufcelik.carplaylab

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Base64
import android.util.Log
import com.shilapi.xcertplay.airplay.*
import com.shilapi.xcertplay.media.AndroidMediaSink
import com.shilapi.xcertplay.mfi.LocalMfiAuthenticationClient
import com.shilapi.xcertplay.orchestration.*
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import java.io.File

/** Owns the radio/media session independently from the foreground Activity's surface. */
class CarPlaySessionService : Service() {
    interface Observer { fun update(message: String, controller: CarPlayController?, sink: AndroidMediaSink?, videoActive: Boolean) }
    private val handler = Handler(Looper.getMainLooper())
    private var controller: CarPlayController? = null
    private var sink: AndroidMediaSink? = null
    private var active = false
    private var connecting = false
    private var stopped = false
    private var retries = 0
    private var attemptStartedMs = 0L
    private var message = "CarPlay hazırlanıyor…"
    private val preferences by lazy { getSharedPreferences("carplay-lab", MODE_PRIVATE) }
    private val retry = Runnable { if (canRetry()) beginAttempt() }
    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (canRetry()) { handler.removeCallbacks(retry); beginAttempt() }
        }
    }
    private fun canRetry() = !stopped && CarPlayReconnectPolicy.shouldRetry(
        preferences.getBoolean("auto-connect", true), selected(this), preferences.getBoolean("paused", false), active, connecting)
    override fun onCreate() {
        super.onCreate(); instance = this
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_USER_PRESENT) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(wakeReceiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(wakeReceiver, filter)
    }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            preferences.edit().putBoolean("paused", true).apply()
            stopSelf(); return START_NOT_STICKY
        }
        if (!selected(this)) { stopSelf(); return START_NOT_STICKY }
        try { promote(intent?.getBooleanExtra("user-visible", false) == true) }
        catch (_: Exception) { publish("Arka plan bağlantısı izni alınamadı. CarPlay ekranını aç."); stopSelf(); return START_NOT_STICKY }
        if (intent?.action == CONNECT) preferences.edit().putBoolean("paused", false).apply()
        if (intent?.action == CONNECT || canRetry()) {
            if (!active && !connecting) beginAttempt()
        }
        return START_STICKY
    }
    private fun promote(userVisible: Boolean) {
        val notifications = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(
            NotificationChannel("carplay-session", "CarPlay bağlantısı", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, CarPlayReceiverActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, CarPlaySessionService::class.java).setAction(STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "carplay-session") else Notification.Builder(this)
        val notification = builder.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Fiorino • CarPlay").setContentText("Kayıtlı iPhone bağlantısı açık")
            .setContentIntent(open).setOngoing(true).addAction(android.R.drawable.ic_media_pause, "Durdur", stop).build()
        if (Build.VERSION.SDK_INT >= 29) {
            var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            // Modern Android requires a visible user action for microphone foreground permission.
            if (userVisible && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            startForeground(812, notification, types)
        } else startForeground(812, notification)
    }
    private fun beginAttempt() {
        if (active || connecting || stopped) return
        handler.removeCallbacks(retry)
        if (retiring?.isAlive == true) {
            handler.postDelayed({ if (!stopped) beginAttempt() }, 150)
            return
        }
        connecting = true
        attemptStartedMs = SystemClock.elapsedRealtime()
        try {
            if (controller != null) { controller!!.reconnectIphone(); return }
            val peer = preferences.getString("iphone", null) ?: error("No selected iPhone")
            val mac = preferences.getString("host-bt-mac", null) ?: error("No receiver address")
            LocalMfiAuthenticationClient.load(File(noBackupFilesDir, LocalMfiAuthenticationClient.DIRECTORY))
            val identity = loadIdentity()
            val size = currentDisplayProfile()
            val media = AndroidMediaSink(surface = null, videoWidth = size.width, videoHeight = size.height,
                context = applicationContext, mainVideoFrameRate = 30, audioFocusEnabled = true, videoPacingDelayMillis = 0)
            sink = media
            val pairings = PairingStore { id, key -> preferences.edit().putString("pair.$id", Base64.encodeToString(key, Base64.NO_WRAP)).apply() }
            preferences.all.filterKeys { it.startsWith("pair.") }.forEach { (key, value) ->
                if (value is String) pairings.save(key.removePrefix("pair."), Base64.decode(value, Base64.NO_WRAP))
            }
            val id = identity.publicKey.take(6).joinToString(":") { "%02X".format(it.toInt() and 255) }
            val config = CarPlayRuntimeConfig(mfiTarget = MfiTarget.LOCAL,
                identification = Iap2IdentificationConfig("Fiorino Auto", "Fiorino", "Fiorino", identity.pairingId, "1.4.1", "1", 0, language = "tr"),
                label = "Fiorino Auto", hostName = "fiorino-carplay", transport = CarPlayTransport.WIRELESS,
                wirelessBluetoothDeviceAddress = peer, wifiP2pPreferredChannel = 6)
            controller = CarPlayController(applicationContext, config,
                AirPlayConfig("Fiorino Auto", id, mac, "220.68",
                    AirPlayDisplayConfig(size.width, size.height, widthPhysicalMm = 250, heightPhysicalMm = size.physicalHeightMm, fps = 30, mainDisplayCornerMasks = true),
                    manufacturer = "Fiorino", model = "Fiorino", oemLabel = "Fiorino Auto", microphone = true),
                identity, pairings, object : AirPlaySessionListener {
                    override val wantsTrace = false
                    override fun onSessionActive(session: AirPlaySession) { post { active = true; connecting = false; retries = 0; publish("CarPlay kontrol oturumu kuruldu.")
                        Log.i("FiorinoCarPlay", "controlReadyMs=${SystemClock.elapsedRealtime() - attemptStartedMs}")
                        if (observers.isEmpty() && Build.VERSION.SDK_INT < 29) runCatching {
                            startActivity(Intent(this@CarPlaySessionService, CarPlayReceiverActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } } }
                    override fun onVideoFrameRendered(session: AirPlaySession) { post { videoActive = true; active = true; connecting = false; retries = 0; publish("CarPlay görüntü akışı başladı."); Log.i("FiorinoCarPlay", "firstFrameMs=${SystemClock.elapsedRealtime() - attemptStartedMs}") } }
                    override fun onSessionEnded(session: AirPlaySession) { post { if (connecting) return@post; active = false; videoActive = false; connecting = false; scheduleRetry("iPhone bağlantısı kesildi.") } }
                    override fun onTransportError(message: String) { post { active = false; connecting = false; scheduleRetry("CarPlay bağlantısı kesildi.") } }
                }, CarPlayMediaEngine(media, microphoneEnabled = true), reportStatus = { status -> post {
                    when (status) {
                        is CarPlayStatus.Failed -> { active = false; videoActive = false; connecting = false; scheduleRetry("CarPlay bağlantısı kurulamadı.") }
                        CarPlayStatus.MfiReady -> publish("Yerel imzalama hazır; iPhone yanıtı bekleniyor.")
                        CarPlayStatus.StartingHotspot -> publish("CarPlay Wi-Fi ağı hazırlanıyor…")
                        is CarPlayStatus.HotspotReady -> publish("Wi-Fi hazır: ${status.band}, kanal ${status.channel}")
                        CarPlayStatus.ConnectingBluetooth -> publish("Kayıtlı iPhone’a bağlanılıyor…")
                        else -> Unit
                    }
                } })
            controller!!.start()
            publish("Kayıtlı iPhone’a otomatik bağlanılıyor…")
        } catch (_: Exception) {
            connecting = false
            publish("CarPlay kurulumu eksik. Alıcı adresini, izinleri ve yerel kimliği kontrol et.")
            // Provisioning errors need setup, not a permanent polling loop.
            stopSelf()
        }
    }
    private fun scheduleRetry(reason: String) {
        handler.removeCallbacks(retry)
        if (!canRetry()) { publish(reason); return }
        val wait = CarPlayReconnectPolicy.retryDelay(retries++)
        publish("$reason ${wait / 1000} saniye sonra tekrar denenecek.")
        handler.postDelayed(retry, wait)
    }
    private fun post(block: () -> Unit) { handler.post { if (!stopped) block() } }
    private fun publish(text: String) { message = text; Log.i("FiorinoCarPlay", text); observers.toList().forEach { it.update(message, controller, sink, videoActive) } }
    private var videoActive = false
    private var displayProfile: CarPlayDisplayProfile? = null
    private fun currentDisplayProfile(): CarPlayDisplayProfile {
        return displayProfile ?: run {
            val width = preferences.getInt("display-width", 0)
            val height = preferences.getInt("display-height", 0)
            val profile = if (width > 0 && height > 0) CarPlayDisplayProfile.fromBounds(width, height) else {
                val metrics = android.util.DisplayMetrics()
                @Suppress("DEPRECATION")
                (getSystemService(WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.getRealMetrics(metrics)
                CarPlayDisplayProfile.fromBounds(metrics.widthPixels, metrics.heightPixels)
            }
            displayProfile = profile
            profile
        }
    }
    private fun loadIdentity(): AirPlayIdentity {
        val file = File(noBackupFilesDir, "airplay-identity.bin")
        if (file.exists()) return java.io.DataInputStream(file.inputStream().buffered()).use { d ->
            AirPlayIdentity(ByteArray(32).also(d::readFully), ByteArray(32).also(d::readFully), d.readUTF()) }
        return AirPlayIdentity.generate().also { id ->
            val atomic = android.util.AtomicFile(file); val stream = atomic.startWrite()
            try { val out = java.io.DataOutputStream(stream); out.write(id.privateKey); out.write(id.publicKey); out.writeUTF(id.pairingId); out.flush(); atomic.finishWrite(stream) }
            catch (e: Exception) { atomic.failWrite(stream); throw e }
        }
    }
    override fun onDestroy() {
        stopped = true; handler.removeCallbacksAndMessages(null); unregisterReceiver(wakeReceiver)
        val old = controller; val oldSink = sink; controller = null; sink = null; active = false; videoActive = false
        publish("CarPlay alıcısı durduruldu."); if (instance === this) instance = null
        val previousRetirement = retiring
        if (old != null || oldSink != null || previousRetirement?.isAlive == true) {
            retiring = Thread {
                previousRetirement?.join()
                try { old?.close() } finally { oldSink?.close() }
            }.also { it.start() }
        }
        super.onDestroy()
    }
    companion object {
        const val CONNECT = "com.yusufcelik.carplay.CONNECT"
        const val STOP = "com.yusufcelik.carplay.STOP"
        private var instance: CarPlaySessionService? = null
        @Volatile private var retiring: Thread? = null
        fun isReleased() = instance == null && retiring?.isAlive != true
        fun isRunning() = instance != null
        fun displayProfile(): CarPlayDisplayProfile? = instance?.displayProfile
        private val observers = linkedSetOf<Observer>()
        fun observe(observer: Observer) { observers.add(observer); instance?.let { observer.update(it.message, it.controller, it.sink, it.videoActive) } }
        fun unobserve(observer: Observer) { observers.remove(observer) }
        fun selected(context: Context) = context.packageName.endsWith(".carplaylab") ||
            context.getSharedPreferences("projection-phones", MODE_PRIVATE).getString("selected-protocol", null) == "CARPLAY"
        fun stop(context: Context) { context.stopService(Intent(context, CarPlaySessionService::class.java)) }
        fun autoStart(context: Context) {
            if (Build.VERSION.SDK_INT < 24 || !selected(context)) return
            if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            val p = context.getSharedPreferences("carplay-lab", MODE_PRIVATE)
            if (!p.getBoolean("auto-connect", false) || p.getBoolean("paused", false) || p.getString("iphone", null) == null) return
            val intent = Intent(context, CarPlaySessionService::class.java)
            try { if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent) }
            catch (_: Exception) { Log.i("FiorinoCarPlay", "Otomatik başlatma Android tarafından ertelendi; CarPlay ekranını aç.") }
        }
    }
}
