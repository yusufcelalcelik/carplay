package com.andrerinas.openheadunit.fiorino

import android.os.Bundle
import com.andrerinas.openheadunit.app.BaseActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.main.settings.SettingItem
import com.andrerinas.openheadunit.main.settings.SettingsAdapter
import com.google.android.material.appbar.MaterialToolbar

/** Uses the same layout, row models and palette as the app's other settings pages. */
abstract class VehiclePreferencesActivity : BaseActivity() {
    protected abstract val titleResource: Int
    protected val settings get() = App.provide(this).settings
    private val preferencesAdapter = SettingsAdapter()
    private lateinit var list: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_audio_stream_settings)
        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setTitle(titleResource)
            setNavigationOnClickListener { finish() }
        }
        list = findViewById(R.id.recycler_view)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = preferencesAdapter
    }

    protected fun showPreferences(items: List<SettingItem>) {
        val position = list.layoutManager?.onSaveInstanceState()
        preferencesAdapter.submitList(items) {
            position?.let { list.layoutManager?.onRestoreInstanceState(it) }
        }
    }
}
