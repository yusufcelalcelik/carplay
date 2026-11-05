package com.andrerinas.openheadunit.fiorino

import android.app.Application
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[25],application=Application::class)
class AutoCarPlayReadyTest {
    private val app get() = RuntimeEnvironment.getApplication()
    @Before fun reset() {
        app.getSharedPreferences("projection-phones",0).edit().clear().commit()
        app.getSharedPreferences("carplay-lab",0).edit().clear().commit()
    }
    @Test fun `direct CarPlay requires a completed saved setup and explicit auto preference`() {
        val p=app.getSharedPreferences("carplay-lab",0)
        app.getSharedPreferences("projection-phones",0).edit().putString("selected-protocol","CARPLAY").commit()
        assertFalse(ProjectionPhonePicker.autoCarPlayReady(app))
        p.edit().putBoolean("auto-connect",true).putString("iphone","AA:BB:CC:DD:EE:02").commit()
        assertFalse(ProjectionPhonePicker.autoCarPlayReady(app))
        p.edit().putString("host-bt-mac","AA:BB:CC:DD:EE:04").commit()
        assertTrue(ProjectionPhonePicker.autoCarPlayReady(app))
        p.edit().putBoolean("paused",true).commit()
        assertFalse(ProjectionPhonePicker.autoCarPlayReady(app))
    }
    @Test fun `selecting Android Auto always wins over saved CarPlay auto setup`() {
        app.getSharedPreferences("carplay-lab",0).edit().putBoolean("auto-connect",true)
            .putString("iphone","AA:BB:CC:DD:EE:02").putString("host-bt-mac","AA:BB:CC:DD:EE:04").commit()
        ProjectionPhonePicker.selectAndroidAuto(app)
        assertFalse(ProjectionPhonePicker.autoCarPlayReady(app))
    }
}
