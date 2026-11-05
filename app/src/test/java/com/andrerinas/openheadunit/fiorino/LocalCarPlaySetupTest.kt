package com.andrerinas.openheadunit.fiorino

import org.junit.Assert.*
import org.junit.Test

class LocalCarPlaySetupTest {
    private val version = "1.4.0-local-auth-test"
    @Test fun `public and ordinary debug builds reject provisioning inputs`() {
        listOf("1.4.0-fiorino-preview", "1.4.0-debug", "1.4.0-local-auth-test-extra").forEach {
            assertNull(LocalCarPlaySetup.hostAddress(it, "50:98:00:00:00:02"))
        }
    }
    @Test fun `only complete unicast non-placeholder radio addresses pass`() {
        listOf(null, "", "bad", "50:98:00:00:00", "50:98:00:00:00:GG", "02:00:00:00:00:00",
            "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF", "51:98:00:00:00:02").forEach {
            assertNull(LocalCarPlaySetup.hostAddress(version, it))
        }
        assertEquals("50:98:AB:CD:EF:02", LocalCarPlaySetup.hostAddress(version, "50:98:ab:cd:ef:02"))
    }
}
