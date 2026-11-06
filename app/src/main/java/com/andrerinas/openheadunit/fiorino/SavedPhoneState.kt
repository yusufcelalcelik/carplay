package com.andrerinas.openheadunit.fiorino

import android.content.Context
import com.andrerinas.openheadunit.utils.Settings
import java.io.File

/** Removes phone choices and AirPlay pairings, retaining receiver provisioning and display settings. */
object SavedPhoneState {
    fun clear(context: Context, settings: Settings) {
        context.getSharedPreferences("usb-bridge", Context.MODE_PRIVATE).edit().clear().commit()
        settings.nativePreferredDeviceMac = ""
        settings.lastConnectedNativeMac = ""
        settings.lastExternalModulePhoneMac = ""
        settings.nativePokeBtMacs = emptySet()
        settings.nativePokeAllPairedDevices = false
        settings.autoStartBluetoothDeviceMacs = emptySet()
        Settings.syncAutoStartBtMacsToDeviceStorage(context, emptySet())
        settings.autoConnectLastSession = false
        context.getSharedPreferences("projection-phones", Context.MODE_PRIVATE).edit().clear().commit()
        val saved = context.getSharedPreferences("carplay-lab", Context.MODE_PRIVATE)
        saved.edit().apply {
            remove("iphone")
            saved.all.keys.filter { it.startsWith("pair.") }.forEach(::remove)
            putBoolean("auto-connect", false); putBoolean("paused", true)
        }.commit()
        File(context.noBackupFilesDir, "airplay-identity.bin").delete()
    }
}
