package com.andrerinas.openheadunit.fiorino

import kotlin.math.roundToInt

/** Head-unit volume steps. No phone transport or AVRCP commands are used here. */
internal class SpeedVolumePolicy {
    private var base = 0
    private var lastApplied = 0
    private var offset = 0
    private var initialized = false

    fun next(current: Int, maximum: Int, speedKmh: Float?, boostSteps: Int, fullBoostSpeedKmh: Int = 90): Int {
        if (!initialized) {
            base = current
            lastApplied = current
            initialized = true
        }
        if (current != lastApplied) {
            // A volume key changes the user's base, even while compensation is active.
            base = if (current == 0) 0 else (current - offset).coerceAtLeast(1)
        }
        val boost = boostAt(speedKmh, boostSteps, fullBoostSpeedKmh)
        val target = if (base == 0) 0 else (base + boost).coerceIn(0, maximum)
        lastApplied = when {
            target > current -> current + 1
            target < current -> current - 1
            else -> current
        }.coerceIn(0, maximum)
        offset = lastApplied - base
        return lastApplied
    }

    fun restored(current: Int): Int = if (initialized && current == lastApplied) base else current

    companion object {
        fun boostAt(speedKmh: Float?, boostSteps: Int, fullBoostSpeedKmh: Int): Int {
            if (speedKmh == null || !speedKmh.isFinite() || speedKmh !in 0f..250f) return 0
            val boostSpeed = fullBoostSpeedKmh.coerceIn(30, 150).toFloat()
            return (speedKmh.coerceAtMost(boostSpeed) / boostSpeed * boostSteps.coerceIn(0, 6)).roundToInt()
        }
    }
}
