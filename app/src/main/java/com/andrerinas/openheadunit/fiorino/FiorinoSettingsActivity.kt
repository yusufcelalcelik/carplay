package com.andrerinas.openheadunit.fiorino

import androidx.appcompat.app.AppCompatActivity
import com.andrerinas.openheadunit.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.*
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.utils.Settings
import com.andrerinas.openheadunit.utils.ToastUtils

/** Small vehicle-specific setup surface. Projection and transports remain in the upstream engine. */
class FiorinoSettingsActivity : AppCompatActivity() {
    private lateinit var settings: Settings
    private lateinit var column: LinearLayout
    private lateinit var screen: VehicleSettingsLayout
    private lateinit var microphoneStatus: TextView
    @Volatile private var testing = false
    @Volatile private var testRecord: AudioRecord? = null
    private val testGeneration = java.util.concurrent.atomic.AtomicInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = App.provide(this).settings
        screen = VehicleSettingsLayout(this, "Fiorino ayarları", "Ses, mikrofon, bağlantı ve ekran ayarları")
        column = screen.section("Ses", "Navigasyon ve müzik", "Anons seviyesi hemen uygulanır. Diğer bağlantı ayarları sonraki bağlantıda kullanılır.", R.drawable.ic_sym_volume_up)
        toggle("Anonslarda müziği otomatik kıs", settings.automaticSpeechDucking) {
            settings.automaticSpeechDucking = it
        }
        val level = text("Anons sırasında müzik %${settings.speechMediaPercent}")
        column.addView(SeekBar(this).apply {
            screen.styleSlider(this)
            max = 100
            progress = settings.speechMediaPercent
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, user: Boolean) {
                    if (user) { settings.speechMediaPercent = value; level.text = "Anons sırasında müzik %$value" }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
        text("Otomatik kısma ortak ses çıkışında çalışır. Gelişmiş ayarlarda ayrı ses kanalları seçilirse kanal yönlendirmesi kullanılır.")
        column = screen.section("Hız", "Hıza göre teyp sesi", "Yol hızına uyum sağlayan kademeli ses.", R.drawable.ic_directions_car)
        toggle("Hız arttıkça teybin medya sesini yükselt", settings.speedVolumeEnabled) {
            settings.speedVolumeEnabled = it
        }
        val speedLevel = text("90 km/sa hızda ek ses ${settings.speedVolumeBoost} kademe")
        column.addView(SeekBar(this).apply {
            screen.styleSlider(this)
            max = 6; progress = settings.speedVolumeBoost
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, user: Boolean) {
                    if (user) { settings.speedVolumeBoost = value; speedLevel.text = "90 km/sa hızda ek ses $value kademe" }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
        text("Teybin GPS hızı kullanılır. Hız azalınca ses başlangıç seviyesine döner. Geçişler iki saniyede en fazla bir kademedir. Elle sessize alma korunur. GPS kaybolunca ek ses kaldırılır. Telefonun ses ayarı için komut gönderilmez. OEM amplifikatörünün Android medya sesine bağlılığı araçta denenmelidir.")
        column = screen.section("Mikrofon", "Mikrofon ayarları", "Otomatik giriş seçimi veya sana uygun elle ayarlar.", R.drawable.ic_mic)
        toggle("Mikrofon girişini otomatik seç", settings.automaticMicrophone) { settings.automaticMicrophone = it }
        button("Mikrofon girişini elle seç") {
            MaterialAlertDialogBuilder(this).setTitle("Mikrofon girişi")
                .setItems(arrayOf("Varsayılan", "Mikrofon", "Ses tanıma", "Sesli iletişim", "Bluetooth SCO")) { _, index ->
                    settings.micInputSource = index
                    settings.automaticMicrophone = false
                    recreate()
                }.show()
        }
        button("Mikrofonu dene — kısa bir cümle söyle") { microphoneTest() }
        microphoneStatus = text("Mikrofon testine hazır.")
        text("Otomatik seçim, açılabilen ses girişini bulur. Sessizlik tek başına arıza sayılmaz. Araç görüşme mikrofonu için aşağıdaki arama testi gerekir.")
        button("Örnekleme hızı uyumluluk ayarı") {
            MaterialAlertDialogBuilder(this).setItems(arrayOf("16 kHz", "48 kHz yedek")) { _, index ->
                settings.micSampleRate = if (index == 0) 16000 else 48000
            }.show()
        }
        text("Telefonun Android Auto mikrofon akışı 16 kHz'dir. Gerekirse 48 kHz kayıt 16 kHz'e dönüştürülür.")
        column = screen.section("Bağlantı", "Telefon ve aramalar", "Eşleştirme, otomatik bağlantı ve arama sesi.", R.drawable.ic_bluetooth_white_24dp)
        button("Telefon eşleştirme ve arka plan bağlantısı") {
            startActivity(Intent(this, BluetoothSetupActivity::class.java))
        }
        text("Telefonda aracın Bluetooth kaydında Telefon aramaları açık olmalı. Park hâlinde gelen ve giden arama dene. Karşılıklı sesi ve arama sonrası müziği kontrol et. HFP görüşme mikrofonunu teybin Bluetooth modülü yönetebilir.")
        button("Bluetooth ayarlarını aç") {
            try { startActivity(Intent(AndroidSettings.ACTION_BLUETOOTH_SETTINGS)) }
            catch (_: Exception) { ToastUtils.showToast(this, "Teybin kendi Bluetooth uygulamasından eşleştir.", Toast.LENGTH_LONG, force = true) }
        }
        column = screen.section("Görüntü", "Görüntü ve ölçek", "Görüntü kalitesi, arayüz boyutu ve DPI.", R.drawable.ic_sym_display_settings)
        toggle("Bağlantı performansına göre sonraki oturumu ayarla", settings.adaptiveVideoQuality) {
            settings.adaptiveVideoQuality = it
        }
        button("Görüntü profili seç") {
            MaterialAlertDialogBuilder(this).setItems(arrayOf("Otomatik — 480p başlangıç", "480p / 30 FPS", "720p / 30 FPS")) { _, index ->
                settings.adaptiveVideoQuality = index == 0
                settings.resolutionId = if (index == 2) 2 else 1
                settings.fpsLimit = 30
                settings.videoCodec = "H.264"
            }.show()
        }
        text("Protokol doğrudan bitrate ayarı sunmuyor. Otomatik mod yeterli görüntü taşıyan oturumlarda kare kaybını ölçer, sonraki bağlantı için 480p veya 720p seçer. Aktif bağlantıyı kesmez.")
        button("Arayüz boyutu / DPI") {
            MaterialAlertDialogBuilder(this).setItems(arrayOf("Otomatik", "Küçük — 140 DPI", "Orta — 160 DPI", "Büyük — 190 DPI", "Özel")) { _, index ->
                if (index < 4) settings.dpiPixelDensity = intArrayOf(0, 140, 160, 190)[index]
                else customDpi()
            }.show()
        }
        column = screen.section("Cihaz", "Teybin özellikleri", "Donanım ve bağlantı yetenekleri.", R.drawable.ic_sym_info)
        val pm = packageManager
        text("Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}\n${Build.MANUFACTURER} ${Build.MODEL}\nİşlemci ${Build.HARDWARE}\nMimariler ${Build.SUPPORTED_ABIS.joinToString()}\nUSB Host ${pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)}\nWi-Fi Direct ${pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)}\nBluetooth ${pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)}\nMikrofon ${pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)}")
        button("Geri dön") { finish() }
    }

    private fun customDpi() {
        val input = EditText(this).apply { inputType = 2; hint = "100–300" }
        MaterialAlertDialogBuilder(this).setTitle("Özel DPI").setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value != null && value in 100..300) settings.dpiPixelDensity = value
                else ToastUtils.showToast(this, "100–300 arasında bir değer gir.", Toast.LENGTH_LONG, force = true)
            }.setNegativeButton("Vazgeç", null).show()
    }

    private fun microphoneTest() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        if (!settings.automaticMicrophone && settings.micInputSource == 4) {
            ToastUtils.showToast(this, "Bluetooth SCO mikrofonunu Android Auto asistanı açıkken dene.", Toast.LENGTH_LONG, force = true)
            return
        }
        if (testing || App.provide(this).commManager.isConnected) {
            ToastUtils.showToast(this, "Mikrofon testinden önce bağlantıyı kapat.", Toast.LENGTH_LONG, force = true)
            return
        }
        testing = true
        val generation = testGeneration.incrementAndGet()
        val result = microphoneStatus.apply { text = "Mikrofon deneniyor. Kısa bir cümle söyle…" }
        Thread {
            var record: AudioRecord? = null
            var message = "Mikrofona erişilemedi."
            try {
                val sources = if (settings.automaticMicrophone) listOf(MediaRecorder.AudioSource.VOICE_COMMUNICATION, MediaRecorder.AudioSource.MIC,
                    MediaRecorder.AudioSource.DEFAULT, MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    else listOf(when (settings.micInputSource) {
                        1 -> MediaRecorder.AudioSource.MIC
                        2 -> MediaRecorder.AudioSource.VOICE_RECOGNITION
                        3 -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
                        else -> MediaRecorder.AudioSource.DEFAULT
                    })
                for (source in sources) {
                    if (generation != testGeneration.get()) break
                    val size = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    if (size <= 0) break
                    record = AudioRecord(source, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(size, 3200))
                    testRecord = record
                    if (generation != testGeneration.get()) break
                    if (record.state != AudioRecord.STATE_INITIALIZED) { record.release(); record = null; continue }
                    record.startRecording()
                    val buffer = ShortArray(1600)
                    var peak = 0
                    var samples = 0
                    while (samples < 32000 && generation == testGeneration.get()) {
                        val count = record.read(buffer, 0, buffer.size)
                        if (count <= 0) break
                        repeat(count) { peak = maxOf(peak, kotlin.math.abs(buffer[it].toInt())) }
                        samples += count
                    }
                    message = "Giriş $source / 16 kHz / tepe seviye %${peak * 100 / 32768}. " +
                        if (peak > 0) "Ses alındı. Netliği araçta asistanla kontrol et." else "Ses alınmadı. Elle başka giriş seçip tekrar dene."
                    break
                }
            } catch (_: Exception) { message = "Mikrofon açılamadı. İzinleri ve giriş seçimini kontrol et." }
            finally {
                try { record?.stop() } catch (_: Exception) { }
                try { record?.release() } catch (_: Exception) { }
                if (testRecord === record) testRecord = null
                if (generation == testGeneration.get()) testing = false
            }
            runOnUiThread {
                if (generation == testGeneration.get() && !isFinishing && !isDestroyed) result.text = message
            }
        }.start()
    }

    override fun onStop() {
        testGeneration.incrementAndGet()
        testing = false
        try { testRecord?.stop() } catch (_: Exception) { }
        super.onStop()
    }
    private fun text(value: String, size: Float = 16f): TextView = screen.label(value, size).apply {
        setPadding(0, screen.dp(10), 0, screen.dp(10))
        column.addView(this)
    }
    private fun button(label: String, action: () -> Unit) {
        column.addView(screen.action(label, clicked = action), LinearLayout.LayoutParams(-1, -2))
    }
    private fun toggle(label: String, value: Boolean, changed: (Boolean) -> Unit) {
        column.addView(screen.toggle(label, value, changed), LinearLayout.LayoutParams(-1, -2))
    }
}
