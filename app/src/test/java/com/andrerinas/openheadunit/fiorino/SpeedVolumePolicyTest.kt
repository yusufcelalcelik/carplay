package com.andrerinas.openheadunit.fiorino

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedVolumePolicyTest {
    @Test fun `speed increases only local steps and falling speed restores base`() {
        val policy = SpeedVolumePolicy()
        assertEquals(6, policy.next(5, 15, 90f, 3))
        assertEquals(7, policy.next(6, 15, 90f, 3))
        assertEquals(8, policy.next(7, 15, 90f, 3))
        assertEquals(7, policy.next(8, 15, 0f, 3))
        assertEquals(6, policy.next(7, 15, null, 3))
        assertEquals(5, policy.next(6, 15, null, 3))
    }
    @Test fun `manual mute is preserved at any speed`() {
        val policy = SpeedVolumePolicy()
        policy.next(5, 15, 90f, 3)
        assertEquals(0, policy.next(0, 15, 90f, 3))
        assertEquals(0, policy.next(0, 15, 90f, 3))
    }
    @Test fun `user adjustment becomes a new baseline and cleanup respects later knob input`() {
        val policy = SpeedVolumePolicy()
        policy.next(5, 15, 90f, 3)
        val next = policy.next(10, 15, 90f, 3)
        assertEquals(11, next)
        assertEquals(9, policy.restored(next))
        assertEquals(12, policy.restored(12))
    }
    @Test fun `maximum is respected and invalid speeds do not boost`() {
        val policy = SpeedVolumePolicy()
        assertEquals(15, policy.next(15, 15, 200f, 6))
        assertEquals(15, policy.next(15, 15, Float.NaN, 6))
    }
}
