package com.andrerinas.openheadunit.utils

import java.security.MessageDigest

/** PIN check with a lockout, so the six digits cannot be walked through. */
class LogViewerAuth(private val pin: String) {
    private var failures = 0
    private var windowStart = 0L
    private var lockedUntil = 0L

    @Synchronized
    fun check(attempt: String?, nowMs: Long): Boolean {
        if (nowMs < lockedUntil) return false
        val ok = attempt != null && MessageDigest.isEqual(
            attempt.toByteArray(Charsets.UTF_8), pin.toByteArray(Charsets.UTF_8)
        )
        if (ok) { failures = 0; return true }
        if (nowMs - windowStart > WINDOW_MS) { windowStart = nowMs; failures = 0 }
        if (++failures >= MAX_FAILURES) { lockedUntil = nowMs + LOCKOUT_MS; failures = 0 }
        return false
    }

    companion object {
        const val MAX_FAILURES = 5
        const val WINDOW_MS = 60_000L
        const val LOCKOUT_MS = 60_000L
    }
}
