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
}
