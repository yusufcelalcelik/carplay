package com.andrerinas.openheadunit.fiorino

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.Settings

/** Lives only during projection. Controls the head unit's STREAM_MUSIC through its own AudioManager. */
internal class SpeedVolumeController(context: Context, private val settings: Settings) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var policy: SpeedVolumePolicy? = null
    private var running = false
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            try {
                if (!settings.speedVolumeEnabled) restore()
                else if (audio.mode == AudioManager.MODE_NORMAL && !audio.isVolumeFixed) {
                    val active = policy ?: SpeedVolumePolicy().also { policy = it }
                    val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val speed = VehicleSpeed.current()
                    val next = active.next(current, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                        speed, settings.speedVolumeBoost, settings.speedVolumeFullBoostKmh)
                    if (current != next) {
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                        val applied = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                        AppLog.d("Fiorino speed volume: GPS km/h=$speed, media before=$current requested=$next applied=$applied")
                    }
                }
            } catch (e: Exception) { AppLog.w("Fiorino speed volume unavailable: ${e.message}") }
            handler.postDelayed(this, 2000)
        }
    }
    fun start() {
        if (running) return
        running = true
        VehicleSpeed.clear()
        handler.post(tick)
    }
    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        restore()
        VehicleSpeed.clear()
    }
    private fun restore() {
        val active = policy ?: return
        policy = null
        try {
            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val restored = active.restored(current)
            if (current != restored && audio.mode == AudioManager.MODE_NORMAL)
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, restored, 0)
        } catch (e: Exception) { AppLog.w("Fiorino volume restore unavailable: ${e.message}") }
    }
}
