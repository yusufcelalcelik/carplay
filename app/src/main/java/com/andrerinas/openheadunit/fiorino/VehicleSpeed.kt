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
        if (speed.isFinite() && speed in 0f..250f)
            reading = Reading(speed, location.elapsedRealtimeNanos / 1_000_000)
    }
    fun current(): Float? = reading?.takeIf {
        SystemClock.elapsedRealtime() - it.atMs in 0..5000
    }?.kmh
    fun clear() { reading = null }
}
