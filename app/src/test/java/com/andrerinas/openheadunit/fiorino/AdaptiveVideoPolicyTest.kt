package com.andrerinas.openheadunit.fiorino

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveVideoPolicyTest {
    @Test fun `short sessions do not change profile`() {
        assertEquals(AdaptiveVideoPolicy.Decision(2, 1), AdaptiveVideoPolicy.decide(2, 1, 100, 80, 0))
    }
    @Test fun `loss falls back and recovery requires three healthy sessions`() {
        val low = AdaptiveVideoPolicy.decide(2, 3, 1000, 50, 0)
        assertEquals(AdaptiveVideoPolicy.Decision(1, 0), low)
        var next = low
        repeat(2) {
            next = AdaptiveVideoPolicy.decide(next.resolutionId, next.healthySessions, 1000, 0, 0)
            assertEquals(1, next.resolutionId)
        }
        next = AdaptiveVideoPolicy.decide(next.resolutionId, next.healthySessions, 1000, 0, 0)
        assertEquals(2, next.resolutionId)
    }
    @Test fun `renderer skips count and intermediate loss breaks healthy streak`() {
        assertEquals(1, AdaptiveVideoPolicy.decide(2, 2, 1000, 0, 60).resolutionId)
        assertEquals(AdaptiveVideoPolicy.Decision(1, 0), AdaptiveVideoPolicy.decide(1, 2, 1000, 20, 0))
    }
}
