package com.yusufcelik.carplaylab

import kotlin.math.abs

object CarPlayViewportPolicy {
    fun size(width: Int, height: Int, profile: CarPlayDisplayProfile, fullscreen: Boolean): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 0 to 0
        val difference = abs(width.toDouble() / height / (profile.width.toDouble() / profile.height) - 1)
        // Absorb tiny codec-block rounding; never stretch a genuinely different screen shape.
        if (fullscreen && difference <= 0.02) return width to height
        val fittedWidth = minOf(width, height * profile.width / profile.height)
        return fittedWidth to fittedWidth * profile.height / profile.width
    }
}
