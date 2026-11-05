package com.andrerinas.openheadunit.fiorino

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

/** Shared vehicle-sized cards and navigation for the Fiorino settings and pairing screens. */
internal class VehicleSettingsLayout(
    private val activity: AppCompatActivity,
    title: String,
    subtitle: String
) {
    companion object {
        val TEXT = Color.rgb(245, 247, 252)
        val MUTED = Color.rgb(173, 185, 205)
        val ACCENT = Color.rgb(255, 164, 98)
        private val CARD = Color.rgb(26, 35, 51)
        private val CONTROL = Color.rgb(43, 55, 76)
    }
    private val scroll = ScrollView(activity).apply {
        isFillViewport = true
        clipToPadding = false
    }
    private val content = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val navigation = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val sections = mutableListOf<Pair<View, MaterialButton>>()
    private var selected: MaterialButton? = null

    init {
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5, 11, 35), Color.rgb(17, 42, 82)))
        }
        val rail = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(12), dp(10), dp(10))
            background = rounded(Color.rgb(25, 31, 42), 28)
        }
        rail.addView(label("FIORINO", 17f, ACCENT).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(14))
        })
        rail.addView(ScrollView(activity).apply { addView(navigation); isVerticalScrollBarEnabled = false },
            LinearLayout.LayoutParams(-1, 0, 1f))
        rail.addView(action("Geri dön", false) { activity.finish() })
        root.addView(rail, LinearLayout.LayoutParams(dp(148), -1))
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(2), 0, 0)
        }
        body.addView(label(title, 30f, TEXT, true))
        body.addView(label(subtitle, 15f, MUTED).apply { setPadding(0, dp(5), 0, dp(16)) })
        scroll.addView(content)
        body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(body, LinearLayout.LayoutParams(0, -1, 1f))
        activity.setContentView(root)
        scroll.viewTreeObserver.addOnScrollChangedListener {
            sections.lastOrNull { it.first.top <= scroll.scrollY + dp(32) }?.second?.let(::select)
        }
    }

    fun section(tab: String, title: String, description: String, icon: Int): LinearLayout {
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(18))
            background = rounded(CARD, 28)
        }
        val heading = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        heading.addView(ImageView(activity).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(ACCENT)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(CONTROL, 16)
            contentDescription = null
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        heading.addView(label(title, 24f, TEXT, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(16) })
        card.addView(heading)
        card.addView(label(description, 15f, MUTED).apply { setPadding(0, dp(14), 0, dp(12)) })
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        val nav = action(tab, false) { scroll.smoothScrollTo(0, card.top) }.apply {
            setIconResource(icon)
            iconTint = ColorStateList.valueOf(TEXT)
            iconSize = dp(20)
            textSize = 14f
            iconPadding = dp(8)
            setPadding(dp(12), 0, dp(12), 0)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        navigation.addView(nav, LinearLayout.LayoutParams(-1, dp(58)).apply { bottomMargin = dp(6) })
        sections.add(card to nav)
        if (selected == null) select(nav)
        return card
    }

    fun label(value: String, size: Float = 16f, color: Int = MUTED, bold: Boolean = false): TextView =
        TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    fun action(value: String, primary: Boolean = false, clicked: () -> Unit): MaterialButton =
        MaterialButton(activity).apply {
            text = value
            textSize = 16f
            isAllCaps = false
            minHeight = dp(60)
            cornerRadius = dp(18)
            insetTop = dp(4)
            insetBottom = dp(4)
            setPadding(dp(18), dp(8), dp(18), dp(8))
            backgroundTintList = ColorStateList.valueOf(if (primary) Color.rgb(176, 72, 8) else CONTROL)
            setTextColor(TEXT)
            setOnClickListener { clicked() }
        }

    fun toggle(value: String, checked: Boolean, changed: (Boolean) -> Unit): SwitchMaterial =
        SwitchMaterial(activity).apply {
            text = value
            textSize = 17f
            setTextColor(TEXT)
            minHeight = dp(64)
            setPadding(0, dp(8), 0, dp(8))
            thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(ACCENT, MUTED))
            trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.rgb(112, 64, 31), CONTROL))
            isChecked = checked
            setOnCheckedChangeListener { _, enabled -> changed(enabled) }
        }

    fun styleSlider(slider: SeekBar) {
        slider.progressTintList = ColorStateList.valueOf(ACCENT)
        slider.thumbTintList = ColorStateList.valueOf(ACCENT)
        slider.progressBackgroundTintList = ColorStateList.valueOf(CONTROL)
        slider.minimumHeight = dp(56)
        slider.setPadding(dp(12), dp(10), dp(12), dp(10))
    }
    fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun select(button: MaterialButton) {
        if (selected === button) return
        selected?.backgroundTintList = ColorStateList.valueOf(CONTROL)
        button.backgroundTintList = ColorStateList.valueOf(Color.rgb(99, 52, 24))
        selected = button
    }
}
