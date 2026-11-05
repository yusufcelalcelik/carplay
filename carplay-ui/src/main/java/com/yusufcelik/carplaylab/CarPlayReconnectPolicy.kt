package com.yusufcelik.carplaylab

/** Bounded retry: a live session is never restarted by a Bluetooth/SCREEN_ON event. */
object CarPlayReconnectPolicy {
    fun retryDelay(attempt: Int): Long = longArrayOf(2_000, 5_000, 10_000, 20_000, 40_000, 60_000)[attempt.coerceIn(0, 5)]
    fun shouldRetry(enabled: Boolean, selected: Boolean, paused: Boolean, active: Boolean, connecting: Boolean) =
        enabled && selected && !paused && !active && !connecting
}
