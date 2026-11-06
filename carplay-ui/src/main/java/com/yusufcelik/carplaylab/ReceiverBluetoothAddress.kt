package com.yusufcelik.carplaylab

/** Validation shared by first connection and the normal settings page. */
object ReceiverBluetoothAddress {
    fun isValid(value: String): Boolean =
        Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}").matches(value) &&
            value.uppercase() !in setOf("02:00:00:00:00:00", "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF") &&
            value.substring(0, 2).toInt(16) and 1 == 0
}
