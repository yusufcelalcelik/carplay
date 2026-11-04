package com.andrerinas.openheadunit.fiorino

/** Choose only at the session boundary. A static map's low FPS is not a fault. */
internal object AdaptiveVideoPolicy {
    data class Decision(val resolutionId: Int, val healthySessions: Int)

    fun decide(resolutionId: Int, healthySessions: Int, fed: Long, dropped: Long, skipped: Long): Decision {
        if (fed < 900) return Decision(resolutionId, healthySessions)
        val loss = (dropped.coerceAtLeast(0) + skipped.coerceAtLeast(0)).toDouble() / fed
        if (loss >= 0.05) return Decision(1, 0)
        if (loss > 0.01) return Decision(resolutionId, 0)
        val streak = (healthySessions + 1).coerceAtMost(3)
        return Decision(if (streak >= 3) 2 else resolutionId, streak)
    }
}
