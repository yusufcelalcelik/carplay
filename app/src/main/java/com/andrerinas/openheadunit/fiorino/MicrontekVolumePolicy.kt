package com.andrerinas.openheadunit.fiorino

/** Some ROMs reflect amplifier commands into the knob setting, others leave the knob untouched. */
internal class MicrontekVolumePolicy {
    private var base: Int? = null
    private var observed: Int? = null
    private var commanded: Int? = null

    fun next(reading: MicrontekVolume.Reading, maximum: Int, boost: Int): Int? {
        if (reading.muted || reading.step == 0 || base == null ||
            (reading.step != observed && reading.step != commanded)) {
            base = reading.step
            commanded = null
        }
        observed = reading.step
        if (reading.muted || reading.step == 0) return null
        val current = commanded ?: reading.step
        val target = (requireNotNull(base) + boost).coerceAtMost(maximum)
        return if (current == target) null else current + target.compareTo(current)
    }

    fun applied(step: Int) {
        commanded = step
    }

    fun restore(reading: MicrontekVolume.Reading): Int? = base?.takeIf {
        !reading.muted && commanded != null && commanded != it &&
            (reading.step == observed || reading.step == commanded)
    }
}
