package com.truckerload.presentation.screens.stats

import kotlin.math.abs

internal fun percentChange(current: Double, previous: Double?): Double? {
    val prev = previous ?: return null
    if (abs(prev) < 0.0001) return null
    return ((current - prev) / prev) * 100.0
}

internal fun formatPct(value: Double): String = "${if (value > 0) "+" else ""}${"%.1f".format(value)}%"
