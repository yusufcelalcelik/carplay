package com.andrerinas.openheadunit.utils

import java.util.TimeZone

/**
 * Recent log lines kept in memory for the Wi-Fi log viewer.
 *
 * Off by default: [offer] returns on one volatile read until [enabled] is set, so a unit that never
 * opens the viewer pays nothing on the log path. Bounded, oldest line dropped first.
 */
object LogRing {
    const val CAPACITY = 4000
    private const val MAX_LINE = 4000

    @Volatile var enabled = false
        private set
    private var users = 0

    private val lock = Any()
    private val lines = ArrayDeque<String>()
    private var lastSeq = 0L

    fun offer(priority: Int, msg: String, nowMs: Long = System.currentTimeMillis()) {
        if (!enabled) return
        val text = if (msg.length > MAX_LINE) msg.substring(0, MAX_LINE) + "…" else msg
        val line = "${clock(nowMs)} ${level(priority)} $text"
        synchronized(lock) {
            lines.addLast(line)
            lastSeq++
            while (lines.size > CAPACITY) lines.removeFirst()
        }
    }

    /** Lines newer than [after] and the sequence number of the newest one held. */
    fun since(after: Long): Pair<Long, List<String>> = synchronized(lock) {
        val first = lastSeq - lines.size + 1
        val skip = (after + 1 - first).coerceIn(0L, lines.size.toLong()).toInt()
        lastSeq to lines.drop(skip)
    }

    /** Capture runs while anyone holds it: the Wi-Fi viewer, the USB flash writer, or both. */
    fun acquire() = synchronized(lock) {
        if (users++ == 0) { lines.clear(); lastSeq = 0L; enabled = true }
    }

    fun release() = synchronized(lock) {
        if (users > 0 && --users == 0) { enabled = false; lines.clear(); lastSeq = 0L }
    }

    private fun level(priority: Int) = when (priority) {
        2 -> 'V'; 3 -> 'D'; 4 -> 'I'; 5 -> 'W'; 6 -> 'E'; else -> '?'
    }

    private fun clock(nowMs: Long): String {
        val local = nowMs + TimeZone.getDefault().getOffset(nowMs)
        val day = ((local % 86_400_000L) + 86_400_000L) % 86_400_000L
        return "%02d:%02d:%02d.%03d".format(
            day / 3_600_000, day / 60_000 % 60, day / 1000 % 60, day % 1000
        )
    }
}
