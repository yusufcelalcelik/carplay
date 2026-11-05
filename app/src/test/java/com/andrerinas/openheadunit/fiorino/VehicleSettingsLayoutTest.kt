package com.andrerinas.openheadunit.fiorino

import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.andrerinas.openheadunit.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25], application = Application::class, qualifiers = "w1024dp-h600dp-land-mdpi")
class VehicleSettingsLayoutTest {
    @Test fun `vehicle navigation fits long Turkish labels and keeps large touch targets`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.FiorinoSettingsTheme)
        controller.setup().use {
            val activity = it.get()
            val screen = VehicleSettingsLayout(activity, "Fiorino ayarları", "Araç ayarları")
            val labels = listOf("Ses", "Hız", "Mikrofon", "Bağlantı", "Görüntü", "Cihaz")
            labels.forEach { label -> screen.section(label, label, "Açıklama", R.drawable.ic_mic) }
            val root = activity.window.decorView
            root.measure(View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, 1024, 600)
            val buttons = descendants(root).filterIsInstance<MaterialButton>().filter { it.text.toString() in labels }
            assertEquals(labels.size, buttons.size)
            buttons.forEach { button ->
                assertTrue("${button.text} touch target", button.height >= 48)
                assertEquals("${button.text} wraps", 1, button.lineCount)
            }
        }
    }

    @Test fun `themed controls preserve callbacks without changing values on creation`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.FiorinoSettingsTheme)
        controller.setup().use {
            val screen = VehicleSettingsLayout(it.get(), "Ayarlar", "")
            var changes = 0
            var enabled = true
            val toggle = screen.toggle("Otomatik mikrofon", true) { value -> changes++; enabled = value }
            assertEquals(0, changes)
            toggle.isChecked = false
            assertEquals(1, changes)
            assertFalse(enabled)
            var clicks = 0
            screen.action("Telefonu seç") { clicks++ }.performClick()
            assertEquals(1, clicks)
        }
    }

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
}
