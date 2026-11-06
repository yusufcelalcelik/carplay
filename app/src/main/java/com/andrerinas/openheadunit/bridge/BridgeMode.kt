package com.andrerinas.openheadunit.bridge

import android.content.Context

object BridgeMode {
    fun enabled(context: Context) = context.getSharedPreferences("projection-phones", 0)
        .getString("selected-protocol", null) == "USB_BRIDGE"
    fun select(context: Context) { context.getSharedPreferences("projection-phones", 0).edit()
        .putString("selected-protocol", "USB_BRIDGE").apply() }
    fun accessorySupported(manufacturer: String?, model: String?) = manufacturer == "Android" &&
        model in setOf("Android Auto", "Android Open Automotive Protocol")
}
