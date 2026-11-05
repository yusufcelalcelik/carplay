package com.yusufcelik.carplaylab

import kotlin.math.max
import kotlin.math.min

/** Match landscape geometry while capping the decoder workload for PX3 and 2.4 GHz Wi-Fi. */
data class CarPlayDisplayProfile(val width: Int, val height: Int) {
    val physicalHeightMm: Int get() = (250.0 * height / width).toInt().coerceAtLeast(1)
    companion object {
        val DEFAULT = CarPlayDisplayProfile(800, 480)
        fun fromBounds(width: Int, height: Int): CarPlayDisplayProfile {
            if (width < 1 || height < 1) return DEFAULT
            val landscapeWidth = max(width, height).toDouble()
            val landscapeHeight = min(width, height).toDouble()
            val scale = min(800.0 / landscapeWidth, 480.0 / landscapeHeight)
            val w = ((landscapeWidth * scale / 16).toInt() * 16).coerceIn(640, 800)
            val h = ((landscapeHeight * scale / 16).toInt() * 16).coerceIn(320, 480)
            return CarPlayDisplayProfile(w, h)
        }
    }
}
