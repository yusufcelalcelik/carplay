package com.andrerinas.openheadunit.fiorino

import com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WirelessMethodPolicyTest {
    @Test fun `native and auto map to their entries`() {
        assertEquals(WirelessMethodPolicy.Method.NATIVE, WirelessMethodPolicy.current(WifiLauncherMode.NATIVE))
        assertEquals(WirelessMethodPolicy.Method.HEADUNIT_SERVER, WirelessMethodPolicy.current(WifiLauncherMode.AUTO))
    }

    @Test fun `helper and manual are reported as other`() {
        assertEquals(WirelessMethodPolicy.Method.OTHER, WirelessMethodPolicy.current(WifiLauncherMode.HELPER))
        assertEquals(WirelessMethodPolicy.Method.OTHER, WirelessMethodPolicy.current(WifiLauncherMode.MANUAL))
    }
}
