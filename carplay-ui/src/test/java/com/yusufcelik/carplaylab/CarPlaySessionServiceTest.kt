package com.yusufcelik.carplaylab

import android.app.Application
import android.content.Context
import android.content.Intent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[25], application=Application::class)
class CarPlaySessionServiceTest {
    private val app get() = RuntimeEnvironment.getApplication()
    @Before fun reset() {
        app.getSharedPreferences("projection-phones", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("carplay-lab", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun `automatic start with no configured phone does not create a service`() {
        app.getSharedPreferences("projection-phones", 0).edit().putString("selected-protocol", "CARPLAY").commit()
        CarPlaySessionService.autoStart(app)
        assertNull(shadowOf(app).nextStartedService)
    }
    @Test fun `Android Auto ownership rejects CarPlay service bring up`() {
        app.getSharedPreferences("projection-phones", 0).edit().putString("selected-protocol", "ANDROID_AUTO").commit()
        val holder=Robolectric.buildService(CarPlaySessionService::class.java).create()
        val service=holder.get()
        service.onStartCommand(Intent(app, CarPlaySessionService::class.java).setAction(CarPlaySessionService.CONNECT),0,1)
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertNull(shadowOf(service).lastForegroundNotification)
        holder.destroy()
    }
    @Test fun `user stop persists a hold across service recreation`() {
        val holder=Robolectric.buildService(CarPlaySessionService::class.java).create()
        holder.get().onStartCommand(Intent().setAction(CarPlaySessionService.STOP),0,1)
        assertTrue(app.getSharedPreferences("carplay-lab", 0).getBoolean("paused", false))
        holder.destroy()
        assertTrue(CarPlaySessionService.isReleased())
    }
}
