package com.truckerload.presentation.screens.stats

import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.math.abs

internal fun percentChange(current: Double, previous: Double?): Double? {
    val prev = previous ?: return null
    if (abs(prev) < 0.0001) return null
    return ((current - prev) / prev) * 100.0
}

internal fun formatPct(value: Double): String = "${if (value > 0) "+" else ""}${"%.1f".format(value)}%"

internal fun formatMoney(value: Double): String {
    return "%,.0f".format(value)
}

internal fun monthShortLabel(month: Int): String {
    val locale = Locale.getDefault()
    val short = DateFormatSymbols(locale).shortMonths.getOrNull((month - 1).coerceIn(0, 11)).orEmpty()
    return short.replace(".", "").replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
