package com.andrerinas.openheadunit.bridge

import android.app.Application
import com.andrerinas.openheadunit.fiorino.ProjectionPhonePicker
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25], application = Application::class)
class BridgeModeTest {
    @Test fun `bridge mode does not masquerade as CarPlay and normal receiver choice restores AA`() {
        val c = RuntimeEnvironment.getApplication()
        c.getSharedPreferences("projection-phones", 0).edit().clear().commit()
        BridgeMode.select(c)
        assertTrue(BridgeMode.enabled(c))
        assertFalse(ProjectionPhonePicker.carPlaySelected(c))
        assertFalse(ProjectionPhonePicker.autoCarPlayReady(c))
        ProjectionPhonePicker.selectAndroidAuto(c)
        assertFalse(BridgeMode.enabled(c))
    }
}
