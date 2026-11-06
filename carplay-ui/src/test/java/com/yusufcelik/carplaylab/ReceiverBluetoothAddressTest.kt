package com.yusufcelik.carplaylab

import org.junit.Assert.*
import org.junit.Test

class ReceiverBluetoothAddressTest {
    @Test fun `reject placeholder broadcast multicast and malformed addresses`() {
        for (v in listOf("02:00:00:00:00:00", "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF", "01:22:33:44:55:66", "AA:BB:CC", "xx:00:11:22:33:44")) assertFalse(v, ReceiverBluetoothAddress.isValid(v))
    }
    @Test fun `accept real format unicast including local addresses`() {
        assertTrue(ReceiverBluetoothAddress.isValid("AA:BB:CC:DD:EE:10"))
        assertTrue(ReceiverBluetoothAddress.isValid("12:34:56:78:9a:bc"))
    }
}
