package com.andrerinas.openheadunit.fiorino

import com.andrerinas.openheadunit.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.widget.*
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.main.settings.SettingItem
import com.andrerinas.openheadunit.utils.ToastUtils

/** Small vehicle-specific setup surface. Projection and transports remain in the upstream engine. */
class FiorinoSettingsActivity : VehiclePreferencesActivity() {
    override val titleResource = R.string.fiorino_settings
    private var microphoneTestMessage = "Mikrofon testine hazır."
    @Volatile private var testing = false
    @Volatile private var testRecord: AudioRecord? = null
    private val testGeneration = java.util.concurrent.atomic.AtomicInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePreferences()
    }

    override fun onResume() {
        super.onResume()
        updatePreferences()
    }

    private fun updatePreferences() {
        val items = mutableListOf<SettingItem>()
        items.add(SettingItem.CategoryHeader("connection", R.string.fiorino_connection))
        items.add(SettingItem.SettingEntry("nativeReceiver", R.string.fiorino_native_setup,
            "Native kablosuz Android Auto, açılışta bağlantı ve yeniden bağlanma") {
            startActivity(Intent(this, BluetoothSetupActivity::class.java))
        })
        items.add(SettingItem.SettingEntry("wirelessProfile", R.string.fiorino_wireless_profile,
            when (settings.wifiDirectBand) { 2 -> "2,4 GHz araç — 480p / 30 FPS"; 1 -> "5 GHz — destekleyen cihazlar"; else -> "Otomatik — 5 GHz, 2,4 GHz yedek" }) {
            MaterialAlertDialogBuilder(this).setTitle(R.string.fiorino_wireless_profile)
                .setItems(arrayOf("Otomatik — destek varsa 5 GHz", "5 GHz — destekleyen cihazlar", "2,4 GHz araç — 480p / 30 FPS")) { _, index ->
                    settings.configureFiorinoWireless(index)
                    updatePreferences()
                }.show()
        })
        items.add(SettingItem.InfoBanner("applyInfo", R.string.fiorino_apply_info))
        items.add(SettingItem.CategoryHeader("sound", R.string.fiorino_sound))
        items.add(SettingItem.ToggleSettingEntry("speechDucking", R.string.fiorino_ducking, null,
            settings.automaticSpeechDucking) { settings.automaticSpeechDucking = it })
        items.add(SettingItem.SliderSettingEntry("speechLevel", R.string.fiorino_speech_level,
            "${settings.speechMediaPercent}%", settings.speechMediaPercent.toFloat(), 0f, 100f, 1f) {
            settings.speechMediaPercent = it.toInt()
        })
        items.add(SettingItem.ToggleSettingEntry("speedVolume", R.string.fiorino_speed_volume,
            R.string.fiorino_speed_description, settings.speedVolumeEnabled) { settings.speedVolumeEnabled = it })
        items.add(SettingItem.SliderSettingEntry("speedBoost", R.string.fiorino_speed_boost,
            "${settings.speedVolumeBoost}", settings.speedVolumeBoost.toFloat(), 0f, 6f, 1f) {
            settings.speedVolumeBoost = it.toInt()
        })
        items.add(SettingItem.CategoryHeader("microphone", R.string.microphone_settings))
        items.add(SettingItem.ToggleSettingEntry("autoMicrophone", R.string.fiorino_auto_microphone, null,
            settings.automaticMicrophone) { settings.automaticMicrophone = it; updatePreferences() })
        val sources = arrayOf("Varsayılan", "Mikrofon", "Ses tanıma", "Sesli iletişim", "Bluetooth SCO")
        items.add(SettingItem.SettingEntry("micSource", R.string.fiorino_mic_source,
            if (settings.automaticMicrophone) "Otomatik" else sources.getOrElse(settings.micInputSource) { "Varsayılan" }) {
            MaterialAlertDialogBuilder(this).setTitle(R.string.fiorino_mic_source).setItems(sources) { _, index ->
                settings.micInputSource = index
                settings.automaticMicrophone = false
                updatePreferences()
            }.show()
        })
        items.add(SettingItem.SettingEntry("micRate", R.string.fiorino_mic_rate,
            "${settings.micSampleRate / 1000} kHz") {
            MaterialAlertDialogBuilder(this).setItems(arrayOf("16 kHz", "48 kHz yedek")) { _, index ->
                settings.micSampleRate = if (index == 0) 16000 else 48000
                updatePreferences()
            }.show()
        })
        items.add(SettingItem.SettingEntry("micTest", R.string.fiorino_mic_test,
            microphoneTestMessage, isEnabled = !testing) { microphoneTest() })
        items.add(SettingItem.CategoryHeader("display", R.string.fiorino_display))
        items.add(SettingItem.ToggleSettingEntry("autoQuality", R.string.fiorino_auto_quality,
            R.string.fiorino_quality_description, settings.adaptiveVideoQuality) { settings.adaptiveVideoQuality = it; updatePreferences() })
        items.add(SettingItem.SettingEntry("videoProfile", R.string.fiorino_video_profile,
            if (settings.adaptiveVideoQuality) "Otomatik" else "${if (settings.resolutionId == 2) "720p" else "480p"} / 30 FPS") {
            MaterialAlertDialogBuilder(this).setItems(arrayOf("Otomatik — 480p başlangıç", "480p / 30 FPS", "720p / 30 FPS")) { _, index ->
                settings.adaptiveVideoQuality = index == 0
                settings.resolutionId = if (index == 2) 2 else 1
                settings.fpsLimit = 30
                settings.videoCodec = "H.264"
                updatePreferences()
            }.show()
        })
        items.add(SettingItem.SettingEntry("density", R.string.fiorino_density,
            if (settings.dpiPixelDensity == 0) "Otomatik" else "${settings.dpiPixelDensity} DPI") {
            MaterialAlertDialogBuilder(this).setItems(arrayOf("Otomatik", "Küçük — 140 DPI", "Orta — 160 DPI", "Büyük — 190 DPI", "Çok büyük — 220 DPI", "Özel")) { _, index ->
                if (index < 5) {
                    settings.dpiPixelDensity = intArrayOf(0, 140, 160, 190, 220)[index]
                    updatePreferences()
                } else customDpi()
            }.show()
        })
        items.add(SettingItem.ToggleSettingEntry("densityOverride", R.string.fiorino_density_override,
            R.string.fiorino_density_override_hint, settings.dpiFloorOverride) {
            settings.dpiFloorOverride = it
        })
        items.add(SettingItem.CategoryHeader("device", R.string.fiorino_device))
        val pm = packageManager
        items.add(SettingItem.InfoBanner("deviceInfo", R.string.fiorino_device,
            "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT} • ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                "${Build.SUPPORTED_ABIS.joinToString()} • USB Host ${pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)} • Wi-Fi Direct ${pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)}"))
        showPreferences(items)
    }

    private fun customDpi() {
        val input = EditText(this).apply { inputType = 2; hint = "100–300" }
        MaterialAlertDialogBuilder(this).setTitle("Özel DPI").setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value != null && value in 100..300) { settings.dpiPixelDensity = value; updatePreferences() }
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
        microphoneTestMessage = "Mikrofon deneniyor. Kısa bir cümle söyle…"
        updatePreferences()
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
                if (generation == testGeneration.get() && !isFinishing && !isDestroyed) {
                    microphoneTestMessage = message
                    updatePreferences()
                }
            }
        }.start()
    }

    override fun onStop() {
        testGeneration.incrementAndGet()
        testing = false
        try { testRecord?.stop() } catch (_: Exception) { }
        super.onStop()
    }
}
