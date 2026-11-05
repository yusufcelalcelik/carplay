package com.andrerinas.openheadunit.fiorino

import android.app.Application
import com.andrerinas.openheadunit.utils.Settings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25], application = Application::class)
class FiorinoSettingsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @Before fun reset() { context.getSharedPreferences(Settings.PREFS_NAME, 0).edit().clear().commit() }

    @Test fun `Android 7 defaults are installed once and preserve user choices`() {
        val settings = Settings(context)
        settings.applyFiorinoDefaults()
        assertEquals(1, settings.resolutionId)
        assertEquals(30, settings.fpsLimit)
        assertTrue(settings.automaticSpeechDucking)
        assertTrue(settings.automaticMicrophone)
        assertTrue(settings.adaptiveVideoQuality)
        assertTrue(settings.autoStartOnBoot)
        assertTrue(settings.autoStartOnScreenOn)
        settings.speechMediaPercent = 55
        settings.dpiPixelDensity = 190
        settings.automaticMicrophone = false
        val reopened = Settings(context)
        reopened.applyFiorinoDefaults()
        assertEquals(55, reopened.speechMediaPercent)
        assertEquals(190, reopened.dpiPixelDensity)
        assertFalse(reopened.automaticMicrophone)
    }
    @Test fun `native receiver setup remembers one phone and preserves audio and display preferences`() {
        val settings = Settings(context)
        settings.speechMediaPercent = 55
        settings.dpiPixelDensity = 190
        settings.automaticMicrophone = false
        settings.autoStartSelfMode = true
        settings.rawKillOnDisconnect = true
        settings.configureNativeReceiver("AA:BB:CC:DD:EE:01")
        assertEquals(com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.NATIVE, settings.wifiConnectionMode)
        assertEquals(setOf(Settings.ConnectionMode.WIFI, Settings.ConnectionMode.USB), settings.connectionModes)
        assertEquals(Settings.ConnectionKind.NATIVE_AA, settings.primaryConnection)
        assertEquals(com.andrerinas.openheadunit.main.HomePrimaryPolicy.Connection.WIFI,
            com.andrerinas.openheadunit.utils.HomeUiHelper.lastConnection(settings))
        assertEquals("AA:BB:CC:DD:EE:01", settings.nativePreferredDeviceMac)
        assertEquals(setOf("AA:BB:CC:DD:EE:01"), settings.autoStartBluetoothDeviceMacs)
        assertEquals(setOf("AA:BB:CC:DD:EE:01"), settings.nativePokeBtMacs)
        assertFalse(settings.nativePokeAllPairedDevices)
        assertTrue(settings.autoStartOnBoot)
        assertTrue(settings.autoStartOnScreenOn)
        assertTrue(settings.autoConnectLastSession)
        assertTrue(settings.rawReopenOnReconnection)
        assertFalse(settings.rawKillOnDisconnect)
        assertFalse(settings.autoStartSelfMode)
        settings.configureNativeReceiver()
        assertEquals("AA:BB:CC:DD:EE:01", settings.nativePreferredDeviceMac)
        assertEquals(55, settings.speechMediaPercent)
        assertEquals(190, settings.dpiPixelDensity)
        assertFalse(settings.automaticMicrophone)
    }

    @Test fun `2_4 GHz vehicle profile pins lightweight video and preserves the selected phone and DPI`() {
        val settings = Settings(context)
        settings.configureNativeReceiver("AA:BB:CC:DD:EE:01")
        settings.dpiPixelDensity = 220
        settings.configureFiorinoWireless(2)
        assertEquals(2, settings.wifiDirectBand)
        assertEquals(2, settings.hotspotBand)
        assertEquals(1, settings.resolutionId)
        assertEquals(30, settings.fpsLimit)
        assertEquals("H.264", settings.videoCodec)
        assertFalse(settings.adaptiveVideoQuality)
        assertTrue(settings.useAacAudio)
        assertTrue(settings.narrowBandProfileCap)
        assertEquals("AA:BB:CC:DD:EE:01", settings.nativePreferredDeviceMac)
        assertEquals(220, settings.dpiPixelDensity)
        settings.configureFiorinoWireless(0)
        assertEquals(0, settings.wifiDirectBand)
        assertTrue(settings.adaptiveVideoQuality)
    }

    @Test fun `invalid phone address cannot partially change the receiver configuration`() {
        val settings = Settings(context)
        settings.wifiConnectionMode = com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.MANUAL
        assertThrows(IllegalArgumentException::class.java) { settings.configureNativeReceiver("invalid") }
        assertEquals(com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.MANUAL, settings.wifiConnectionMode)
        assertFalse(settings.autoStartOnBoot)
    }

    @Test fun `explicit Native setup cannot be reverted to the developer server by legacy flags`() {
        val prefs = context.getSharedPreferences(Settings.PREFS_NAME, 0)
        prefs.edit().putBoolean("wifi-launcher-mode", false).commit()
        val settings = Settings(context)
        settings.configureNativeReceiver("AA:BB:CC:DD:EE:01")
        repeat(3) {
            assertEquals(com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.NATIVE, settings.wifiConnectionMode)
        }
        assertFalse(prefs.contains("wifi-launcher-mode"))
        assertFalse(prefs.contains("native-aa-wireless"))
    }

    @Test fun `an existing canonical mode wins over stale legacy values`() {
        val prefs = context.getSharedPreferences(Settings.PREFS_NAME, 0)
        prefs.edit().putInt("wifi-connection-mode", 3).putBoolean("wifi-launcher-mode", false).commit()
        assertEquals(com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.NATIVE, Settings(context).wifiConnectionMode)
        prefs.edit().putInt("wifi-connection-mode", 1).putBoolean("native-aa-wireless", true).commit()
        assertEquals(com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.AUTO, Settings(context).wifiConnectionMode)
    }

    @Test fun `legacy modes migrate once and remain stable`() {
        val prefs = context.getSharedPreferences(Settings.PREFS_NAME, 0)
        for (helper in listOf(false, true)) {
            prefs.edit().clear().putBoolean("wifi-launcher-mode", helper).commit()
            val expected = if (helper) com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.HELPER
                else com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.AUTO
            val settings = Settings(context)
            repeat(2) { assertEquals(expected, settings.wifiConnectionMode) }
            assertFalse(prefs.contains("wifi-launcher-mode"))
        }
        prefs.edit().clear().putBoolean("wifi-launcher-mode", false).putBoolean("native-aa-wireless", true).commit()
        val settings = Settings(context)
        repeat(2) {
            assertEquals(com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.NATIVE, settings.wifiConnectionMode)
        }
    }

}
