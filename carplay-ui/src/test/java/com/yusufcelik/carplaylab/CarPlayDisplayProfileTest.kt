package com.yusufcelik.carplaylab
import org.junit.Assert.*
import org.junit.Test
class CarPlayDisplayProfileTest {
    @Test fun `ultrawide phone and ordinary radio keep their own landscape geometry`() {
        assertEquals(CarPlayDisplayProfile(800,368),CarPlayDisplayProfile.fromBounds(2340,1080))
        assertEquals(CarPlayDisplayProfile(800,368),CarPlayDisplayProfile.fromBounds(1080,2340))
        assertEquals(CarPlayDisplayProfile(800,464),CarPlayDisplayProfile.fromBounds(1024,600))
        assertEquals(CarPlayDisplayProfile(800,480),CarPlayDisplayProfile.fromBounds(800,480))
    }
    @Test fun `invalid geometry falls back and pathological screens stay within decoder limits`() {
        assertEquals(CarPlayDisplayProfile.DEFAULT,CarPlayDisplayProfile.fromBounds(0,0))
        for ((w,h) in listOf(1 to 1, 8000 to 100, 640 to 480, 1920 to 1080)) {
            val p=CarPlayDisplayProfile.fromBounds(w,h)
            assertTrue(p.width in 640..800 && p.height in 320..480)
            assertEquals(0,p.width%16);assertEquals(0,p.height%16)
        }
    }
}
