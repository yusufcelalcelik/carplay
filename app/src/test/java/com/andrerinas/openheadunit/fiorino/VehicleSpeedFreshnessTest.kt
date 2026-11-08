package com.andrerinas.openheadunit.fiorino

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleSpeedFreshnessTest {
    @Test fun `legacy provider with no elapsed timestamp can contribute a fresh fix`() {
        assertEquals(9_500L, VehicleSpeed.fixElapsedMs(0, 99_500, 10_000, 100_000))
        assertNull(VehicleSpeed.fixElapsedMs(0, 90_000, 10_000, 100_000))
        assertNull(VehicleSpeed.fixElapsedMs(0, 101_000, 10_000, 100_000))
    }

    @Test fun `fresh wall time cannot renew a stale monotonic fix`() {
        assertNull(VehicleSpeed.fixElapsedMs(1_000_000_000, 100_000, 10_000, 100_000))
    }
}
