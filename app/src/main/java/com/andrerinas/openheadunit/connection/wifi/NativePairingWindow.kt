package com.andrerinas.openheadunit.connection.wifi

/** One explicit setup visit may keep Native AA listening while settings are visible. */
class NativePairingWindow {
    companion object { const val DURATION_MS = 180_000L }
    private var owner: Any? = null
    private var openedAt = 0L
    @Synchronized fun open(token: Any, nowMs: Long) { owner = token; openedAt = nowMs }
    @Synchronized fun isOpen(nowMs: Long): Boolean = owner != null && nowMs - openedAt in 0 until DURATION_MS
    @Synchronized fun close(token: Any): Boolean {
        if (owner !== token) return false
        owner = null
        return true
    }
}
