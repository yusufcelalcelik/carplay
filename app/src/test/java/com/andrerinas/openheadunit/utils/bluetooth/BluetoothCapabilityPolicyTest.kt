package com.andrerinas.openheadunit.utils.bluetooth

import com.andrerinas.openheadunit.utils.bluetooth.BluetoothCapabilityPolicy.Advice
import com.andrerinas.openheadunit.utils.bluetooth.BluetoothCapabilityPolicy.Verdict
import org.junit.Assert.*
import org.junit.Test

/** Each case is a head unit family, reduced to what the app can observe on it. */
class BluetoothCapabilityPolicyTest {

    @Test fun `MTCD PX3 - adapter object exists, enable is accepted, radio never comes on`() {
        val v = BluetoothCapabilityPolicy.classify(adapterPresent = true, enableAccepted = true, reachedOn = false)
        assertEquals(Verdict.NEVER_TURNS_ON, v)
        assertEquals(Advice.USB_BRIDGE, BluetoothCapabilityPolicy.advice(v, moduleServiceAvailable = false))
    }

    @Test fun `HCT unit with a reachable module service is pointed at the module route`() {
        val v = BluetoothCapabilityPolicy.classify(adapterPresent = true, enableAccepted = false)
        assertEquals(Verdict.NEVER_TURNS_ON, v)
        assertEquals(Advice.MODULE_ROUTE, BluetoothCapabilityPolicy.advice(v, moduleServiceAvailable = true))
    }

    @Test fun `unit with no adapter at all`() {
        assertEquals(Verdict.NO_ADAPTER, BluetoothCapabilityPolicy.classify(adapterPresent = false))
    }

    @Test fun `shimmed radio that turns on but cannot publish a record`() {
        val v = BluetoothCapabilityPolicy.classify(adapterPresent = true, reachedOn = true, listenFailures = 4)
        assertEquals(Verdict.NO_RFCOMM_SERVER, v)
    }

    @Test fun `a few listen failures on a working radio are not a verdict`() {
        assertEquals(Verdict.UNKNOWN,
            BluetoothCapabilityPolicy.classify(adapterPresent = true, reachedOn = true, listenFailures = 3))
    }

    @Test fun `ordinary tablet - listener opens`() {
        val v = BluetoothCapabilityPolicy.classify(adapterPresent = true, reachedOn = true, listenOpened = true)
        assertEquals(Verdict.WORKING, v)
        assertEquals(Advice.NONE, BluetoothCapabilityPolicy.advice(v, moduleServiceAvailable = false))
    }

    @Test fun `a radio that is merely off with nothing tried stays unknown`() {
        assertEquals(Verdict.UNKNOWN, BluetoothCapabilityPolicy.classify(adapterPresent = true))
    }

    @Test fun `an observation without a verdict keeps the earlier one, and success replaces failure`() {
        assertEquals(Verdict.NEVER_TURNS_ON, BluetoothCapabilityPolicy.merge(Verdict.NEVER_TURNS_ON, Verdict.UNKNOWN))
        assertEquals(Verdict.WORKING, BluetoothCapabilityPolicy.merge(Verdict.NEVER_TURNS_ON, Verdict.WORKING))
    }

    @Test fun `a single failure never overwrites working`() {
        for (f in listOf(Verdict.NO_ADAPTER, Verdict.NEVER_TURNS_ON, Verdict.NO_RFCOMM_SERVER)) {
            assertEquals(Verdict.WORKING, BluetoothCapabilityPolicy.merge(Verdict.WORKING, f))
            assertEquals(f, BluetoothCapabilityPolicy.merge(Verdict.UNKNOWN, f))
            assertEquals(f, BluetoothCapabilityPolicy.merge(Verdict.NO_RFCOMM_SERVER, f))
        }
        assertEquals(Verdict.WORKING, BluetoothCapabilityPolicy.merge(Verdict.WORKING, Verdict.UNKNOWN))
        assertEquals(Verdict.WORKING, BluetoothCapabilityPolicy.merge(Verdict.UNKNOWN, Verdict.WORKING))
    }

    @Test fun `system consent is the default and in-app enable needs a reason`() {
        assertFalse(BluetoothCapabilityPolicy.enablesInApp(25, knownHangingFirmware = false, systemConsentFailedBefore = false))
        assertTrue(BluetoothCapabilityPolicy.enablesInApp(25, knownHangingFirmware = true, systemConsentFailedBefore = false))
        assertTrue(BluetoothCapabilityPolicy.enablesInApp(29, knownHangingFirmware = false, systemConsentFailedBefore = true))
        assertFalse(BluetoothCapabilityPolicy.enablesInApp(33, knownHangingFirmware = true, systemConsentFailedBefore = true))
    }

    @Test fun `stored verdict survives and garbage is unknown`() {
        assertEquals(Verdict.NO_ADAPTER, BluetoothCapabilityPolicy.parse("NO_ADAPTER"))
        assertEquals(Verdict.UNKNOWN, BluetoothCapabilityPolicy.parse("x"))
        assertEquals(Verdict.UNKNOWN, BluetoothCapabilityPolicy.parse(null))
    }
}
