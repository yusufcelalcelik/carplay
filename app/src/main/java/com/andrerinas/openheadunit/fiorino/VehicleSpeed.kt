package com.andrerinas.openheadunit.fiorino

import android.location.Location
import android.os.SystemClock

/** Only live GPS callbacks feed this. Resent navigation fixes cannot refresh speed freshness. */
internal object VehicleSpeed {
    private data class Reading(val kmh: Float, val atMs: Long)
    @Volatile private var reading: Reading? = null
    fun update(location: Location) {
        if (!location.hasSpeed() || location.isFromMockProvider ||
            (location.hasAccuracy() && location.accuracy > 50f)) return
        val speed = location.speed * 3.6f
        val atMs = fixElapsedMs(location.elapsedRealtimeNanos, location.time,
            SystemClock.elapsedRealtime(), System.currentTimeMillis()) ?: return
        if (speed.isFinite() && speed in 0f..250f) reading = Reading(speed, atMs)
    }

    internal fun fixElapsedMs(nanos: Long, wallMs: Long, elapsedNow: Long, wallNow: Long): Long? {
        if (nanos > 0) return (nanos / 1_000_000).takeIf { elapsedNow - it in 0..5000 }
        // Legacy providers may omit elapsed time. Use wall time only for a fresh real fix.
        val age = wallNow - wallMs
        return (elapsedNow - age).takeIf { wallMs > 0 && age in 0..5000 && it >= 0 }
    }
    fun current(): Float? = reading?.takeIf {
        SystemClock.elapsedRealtime() - it.atMs in 0..5000
    }?.kmh
    fun clear() { reading = null }
}
