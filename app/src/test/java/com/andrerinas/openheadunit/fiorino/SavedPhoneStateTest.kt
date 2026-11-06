package com.andrerinas.openheadunit.fiorino

import android.app.Application
import com.andrerinas.openheadunit.utils.Settings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[25], application=Application::class)
class SavedPhoneStateTest {
    @Test fun `forgetting phones preserves receiver address geometry and hardware provisioning`() {
        val app=RuntimeEnvironment.getApplication()
        val settings=Settings(app)
        settings.nativePreferredDeviceMac="AA:BB:CC:DD:EE:02"
        settings.lastConnectedNativeMac="AA:BB:CC:DD:EE:02"
        settings.lastExternalModulePhoneMac="AA:BB:CC:DD:EE:08"
        settings.nativePokeBtMacs=setOf("AA:BB:CC:DD:EE:02")
        settings.autoStartBluetoothDeviceMacs=setOf("AA:BB:CC:DD:EE:02")
        Settings.syncAutoStartBtMacsToDeviceStorage(app,setOf("AA:BB:CC:DD:EE:02"))
        val saved=app.getSharedPreferences("carplay-lab",0)
        saved.edit().putString("iphone","AA:BB:CC:DD:EE:04").putString("pair.test","public-key")
            .putString("host-bt-mac","AA:BB:CC:DD:EE:06").putInt("display-width",800).putInt("display-height",368).commit()
        val hardware=File(app.noBackupFilesDir,"offline-mfi/test-marker").apply { parentFile.mkdirs();writeText("receiver-provisioning") }
        File(app.noBackupFilesDir,"airplay-identity.bin").writeText("old-phone-pairing")
        app.getSharedPreferences("usb-bridge",0).edit().putString("phone","AA:BB:CC:DD:EE:10").commit()
        SavedPhoneState.clear(app,settings)
        assertFalse(app.getSharedPreferences("usb-bridge",0).contains("phone"))
        assertNull(saved.getString("iphone",null));assertFalse(saved.contains("pair.test"))
        assertEquals("AA:BB:CC:DD:EE:06",saved.getString("host-bt-mac",null))
        assertEquals(368,saved.getInt("display-height",0))
        assertTrue(hardware.exists());assertFalse(File(app.noBackupFilesDir,"airplay-identity.bin").exists())
        assertEquals("",settings.nativePreferredDeviceMac);assertTrue(settings.nativePokeBtMacs.isEmpty())
        assertEquals("",settings.lastExternalModulePhoneMac)
        assertTrue(Settings.getAutoStartBtMacs(app).isEmpty())
        assertFalse(settings.autoConnectLastSession);assertTrue(saved.getBoolean("paused",false))
    }
}
