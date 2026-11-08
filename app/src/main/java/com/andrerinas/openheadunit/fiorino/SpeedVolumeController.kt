package com.andrerinas.openheadunit.fiorino

import android.content.Context
import android.media.AudioManager
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Owns the receiver volume while Android Auto is connected. */
internal class SpeedVolumeController(context: Context, private val settings: Settings, private val scope: CoroutineScope) {
    enum class Output { ANDROID, MICRONTEK, UNAVAILABLE }

    companion object {
        @Volatile var output: Output? = null
            private set
    }

    private val appContext = context.applicationContext
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val ownership = Mutex()
    private var worker: Job? = null

    fun start() {
        if (worker?.isActive == true) return
        VehicleSpeed.clear()
        worker = scope.launch(Dispatchers.IO) {
            // Finish restoring the previous session before a reconnect can take over the volume.
            ownership.withLock {
                val microntek = runCatching { MicrontekVolume.open(appContext) }.getOrElse {
                    AppLog.i("Fiorino speed volume: Microntek probe unavailable (${it.javaClass.simpleName})")
                    null
                }
                ensureActive()
                output = if (microntek != null) Output.MICRONTEK
                    else if (audio.isVolumeFixed) Output.UNAVAILABLE else Output.ANDROID
                AppLog.i("Fiorino speed volume output: $output")
                var androidPolicy: SpeedVolumePolicy? = null
                var microntekPolicy: MicrontekVolumePolicy? = null

                fun restore() {
                    try {
                        if (audio.mode != AudioManager.MODE_NORMAL) return
                        if (microntek != null) {
                            microntekPolicy?.let { it.restore(microntek.read()) }?.let(microntek::write)
                        } else {
                            androidPolicy?.let { policy ->
                                val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val original = policy.restored(current)
                                if (current != original) audio.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0)
                            }
                        }
                    } catch (e: Exception) {
                        AppLog.w("Fiorino volume restore unavailable: ${e.javaClass.simpleName}")
                    } finally {
                        androidPolicy = null
                        microntekPolicy = null
                    }
                }

                try {
                    while (isActive) {
                        if (audio.mode == AudioManager.MODE_NORMAL) {
                            if (!settings.speedVolumeEnabled) {
                                restore()
                            } else {
                                val speed = VehicleSpeed.current()
                                val boost = settings.speedVolumeBoost
                                val referenceSpeed = settings.speedVolumeFullBoostKmh
                                if (microntek != null) {
                                    val policy = microntekPolicy ?: MicrontekVolumePolicy().also { microntekPolicy = it }
                                    val reading = microntek.read()
                                    val next = policy.next(reading, microntek.maximum,
                                        SpeedVolumePolicy.boostAt(speed, boost, referenceSpeed))
                                    if (next != null) {
                                        ensureActive()
                                        microntek.write(next)
                                        policy.applied(next)
                                        AppLog.d("Fiorino speed volume: GPS km/h=$speed, Microntek command step=$next")
                                    }
                                } else if (!audio.isVolumeFixed) {
                                    val policy = androidPolicy ?: SpeedVolumePolicy().also { androidPolicy = it }
                                    val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val next = policy.next(current, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                                        speed, boost, referenceSpeed)
                                    if (current != next) {
                                        ensureActive()
                                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                        val applied = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        AppLog.d("Fiorino speed volume: GPS km/h=$speed, Android requested=$next applied=$applied")
                                    }
                                }
                            }
                        }
                        delay(2000)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    output = Output.UNAVAILABLE
                    AppLog.w("Fiorino speed volume stopped: ${e.message}")
                } finally {
                    restore()
                    if (!isActive) output = null
                }
            }
        }
    }

    fun stop() {
        worker?.cancel()
        output = null
        VehicleSpeed.clear()
    }
}
