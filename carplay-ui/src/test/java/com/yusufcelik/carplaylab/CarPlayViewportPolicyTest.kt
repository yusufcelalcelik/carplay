package com.yusufcelik.carplaylab
import org.junit.Assert.*
import org.junit.Test
class CarPlayViewportPolicyTest {
    @Test fun `codec block rounding leaves no visible bars in fullscreen`() {
        assertEquals(2340 to 1080, CarPlayViewportPolicy.size(2340,1080,CarPlayDisplayProfile(800,368),true))
    }
    @Test fun `a genuinely mismatched canvas preserves its shape instead of cropping controls`() {
        assertEquals(1800 to 1080, CarPlayViewportPolicy.size(2340,1080,CarPlayDisplayProfile(800,480),true))
        assertEquals(0 to 0, CarPlayViewportPolicy.size(0,0,CarPlayDisplayProfile.DEFAULT,true))
    }
}
