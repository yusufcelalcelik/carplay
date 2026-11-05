package com.andrerinas.openheadunit.connection.wifi

import org.junit.Assert.*
import org.junit.Test

class NativePairingWindowTest {
    @Test fun `setup exposes the receiver only during an explicitly owned bounded window`() {
        val window = NativePairingWindow()
        val owner = Any()
        assertFalse(window.isOpen(100))
        window.open(owner, 100)
        assertTrue(window.isOpen(100))
        assertTrue(window.isOpen(100 + NativePairingWindow.DURATION_MS - 1))
        assertFalse(window.isOpen(100 + NativePairingWindow.DURATION_MS))
        assertFalse(window.isOpen(99))
        assertTrue(window.close(owner))
        assertFalse(window.isOpen(101))
    }
    @Test fun `late destruction of an old setup page cannot close the new page window`() {
        val window = NativePairingWindow()
        val old = Any(); val newer = Any()
        window.open(old, 100)
        window.open(newer, 200)
        assertFalse(window.close(old))
        assertTrue(window.isOpen(201))
        assertTrue(window.close(newer))
        assertFalse(window.isOpen(201))
    }
    @Test fun `phone-led setup keeps settings from stopping the Native service record`() {
        assertFalse(SettingsScreenPausePolicy.pauses(true, false, false, nativePairing = true))
        assertTrue(SettingsScreenPausePolicy.rearmsOnRelease(false, false, nativePairing = true))
        assertTrue(SettingsScreenPausePolicy.pauses(true, false, false, nativePairing = false))
        assertFalse(SettingsScreenPausePolicy.pauses(true, true, false, nativePairing = false))
    }
}
