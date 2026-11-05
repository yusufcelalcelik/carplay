package com.yusufcelik.carplaylab
import org.junit.Assert.*
import org.junit.Test
class CarPlayReconnectPolicyTest {
    @Test fun `retries back off and remain bounded`() {
        assertEquals(listOf(2000L,5000L,10000L,20000L,40000L,60000L,60000L), (0..6).map(CarPlayReconnectPolicy::retryDelay))
        assertEquals(60000L, CarPlayReconnectPolicy.retryDelay(Int.MAX_VALUE))
    }
    @Test fun `active connecting stopped and unselected sessions never reconnect`() {
        for (flags in 0 until 32) {
            val enabled = flags and 1 != 0; val selected = flags and 2 != 0; val paused = flags and 4 != 0
            val active = flags and 8 != 0; val connecting = flags and 16 != 0
            assertEquals(flags == 3, CarPlayReconnectPolicy.shouldRetry(enabled,selected,paused,active,connecting))
        }
    }
}
