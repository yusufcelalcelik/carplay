package com.yusufcelik.carplaylab

import android.Manifest
import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.io.File
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
@Config(sdk = [25], application = Application::class)
class CarPlayReceiverActivityTest {
    private val app get() = RuntimeEnvironment.getApplication()
    @Before fun reset() {
        app.getSharedPreferences("carplay-lab", 0).edit().clear()
            .putString("iphone", "AA:BB:CC:DD:EE:02").commit()
        File(app.noBackupFilesDir, "offline-mfi").deleteRecursively()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO)
    }
    private fun all(v: View): List<View> = listOf(v) +
        if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun tryStart(): String {
        val holder = Robolectric.buildActivity(CarPlayReceiverActivity::class.java).setup()
        val a = holder.get()
        val views = all(a.findViewById(android.R.id.content))
        views.filterIsInstance<Button>().single { it.text == "CarPlay başlat" }.performClick()
        val texts = views.filterIsInstance<TextView>().joinToString("\n") { it.text.toString() }
        holder.pause().stop().destroy()
        return texts
    }
    @Test fun `host phone selection overrides the standalone laboratory favorite`() {
        val intent = android.content.Intent(app, CarPlayReceiverActivity::class.java)
            .putExtra("selectedPhoneMac", "AA:BB:CC:DD:EE:04")
        val holder = Robolectric.buildActivity(CarPlayReceiverActivity::class.java, intent).setup()
        assertEquals("AA:BB:CC:DD:EE:04", app.getSharedPreferences("carplay-lab", 0).getString("iphone", null))
        holder.pause().stop().destroy()
    }
    @Test @Config(sdk = [31]) fun `fullscreen retains edge layout flags and permits camera cutouts`() {
        val holder = Robolectric.buildActivity(CarPlayReceiverActivity::class.java).setup()
        val activity = holder.get()
        assertEquals(android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            activity.window.attributes.layoutInDisplayCutoutMode)
        val flags = activity.window.decorView.systemUiVisibility
        assertTrue(flags and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION != 0)
        assertTrue(flags and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN != 0)
        holder.pause().stop().destroy()
    }
    @Test fun `source build with no accessory identity never announces CarPlay ready`() {
        val text = tryStart()
        assertTrue(text.contains("Yerel aksesuar kimliği yok veya geçersiz"))
        assertTrue(text.contains("CarPlay hazır diye bildirilmedi"))
        assertFalse(text.contains("görüntü akışı başladı"))
    }
    @Test fun `malformed local provisioning fails before starting the wireless receiver`() {
        val dir = File(app.noBackupFilesDir, "offline-mfi").apply { mkdirs() }
        File(dir, "identity.pk8").writeBytes(byteArrayOf(1,2,3))
        File(dir, "certificate.p7b").writeBytes(byteArrayOf(4,5,6))
        assertTrue(tryStart().contains("Yerel aksesuar kimliği yok veya geçersiz"))
    }
}
