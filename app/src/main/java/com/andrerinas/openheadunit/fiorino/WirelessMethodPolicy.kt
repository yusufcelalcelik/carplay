package com.andrerinas.openheadunit.fiorino

import com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode

/** Which wireless method picker entry matches the stored mode, so the subtitle stays honest. */
object WirelessMethodPolicy {
    enum class Method { NATIVE, HEADUNIT_SERVER, OTHER }

    fun current(mode: WifiLauncherMode): Method = when (mode) {
        WifiLauncherMode.NATIVE -> Method.NATIVE
        WifiLauncherMode.AUTO -> Method.HEADUNIT_SERVER
        else -> Method.OTHER
    }
}
