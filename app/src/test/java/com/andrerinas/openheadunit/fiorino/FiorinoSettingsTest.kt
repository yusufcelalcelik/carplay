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

    @Test fun `invalid phone address cannot partially change the receiver configuration`() {
        val settings = Settings(context)
        settings.wifiConnectionMode = com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.MANUAL
        assertThrows(IllegalArgumentException::class.java) { settings.configureNativeReceiver("invalid") }
        assertEquals(com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.MANUAL, settings.wifiConnectionMode)
        assertFalse(settings.autoStartOnBoot)
    }

}
