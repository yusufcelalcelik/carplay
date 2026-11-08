package com.andrerinas.openheadunit.fiorino

import com.andrerinas.openheadunit.fiorino.MicrontekVolume.Reading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MicrontekVolumePolicyTest {
    @Test fun `knob and mirrored amplifier steps do not stack the boost`() {
        val policy = MicrontekVolumePolicy()
        assertEquals(9, policy.next(Reading(8, false), 30, 3))
        policy.applied(9)
        assertEquals(10, policy.next(Reading(8, false), 30, 3))
        policy.applied(10)
        assertEquals(11, policy.next(Reading(10, false), 30, 3))
        policy.applied(11)
        assertNull(policy.next(Reading(11, false), 30, 3))
        assertEquals(8, policy.restore(Reading(11, false)))
    }

    @Test fun `manual knob change and mute override an automatic command`() {
        val policy = MicrontekVolumePolicy()
        policy.next(Reading(8, false), 30, 3)
        policy.applied(9)
        assertNull(policy.restore(Reading(4, false)))
        assertEquals(5, policy.next(Reading(4, false), 30, 3))
        policy.applied(5)
        assertNull(policy.next(Reading(4, true), 30, 3))
        assertNull(policy.restore(Reading(4, true)))
    }

    @Test fun `amplifier scale matches MTCD and clamps via logical volume maximum`() {
        assertEquals(25, MicrontekVolume.amplifierLevel(5, 30))
        assertEquals(50, MicrontekVolume.amplifierLevel(12, 30))
        assertEquals(100, MicrontekVolume.amplifierLevel(30, 30))
        val policy = MicrontekVolumePolicy()
        assertNull(policy.next(Reading(30, false), 30, 6))
    }
}
