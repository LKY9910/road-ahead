package com.roadahead.core.text

import kotlin.math.roundToInt

fun koDistance(meters: Double): String = when {
    meters < 1000 -> {
        val rounded = (meters / 100).roundToInt().coerceAtLeast(1) * 100
        if (rounded >= 1000) "1km" else "${rounded}m"
    }
    meters < 10_000 -> {
        val tenths = (meters / 100).roundToInt()
        if (tenths % 10 == 0) "${tenths / 10}km" else "${tenths / 10}.${tenths % 10}km"
    }
    else -> "${(meters / 1000).roundToInt()}km"
}
