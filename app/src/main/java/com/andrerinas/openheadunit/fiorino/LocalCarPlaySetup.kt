package com.andrerinas.openheadunit.fiorino

/** ADB provisioning of a public radio address is accepted only by explicit local-auth test builds. */
object LocalCarPlaySetup {
    const val HOST_ADDRESS_EXTRA = "fiorinoLocalHostBluetoothMac"
    fun hostAddress(versionName: String, value: String?): String? {
        if (!versionName.endsWith("-local-auth-test")) return null
        val mac = value?.uppercase() ?: return null
        if (!Regex("[0-9A-F]{2}(:[0-9A-F]{2}){5}").matches(mac)) return null
        if (mac in setOf("02:00:00:00:00:00", "00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF")) return null
        if (mac.substring(0, 2).toInt(16) and 1 != 0) return null
        return mac
    }
}
