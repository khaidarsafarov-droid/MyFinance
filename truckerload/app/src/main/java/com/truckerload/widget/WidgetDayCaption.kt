package com.truckerload.widget

/** Captions under Sun–Sat chips: earnings, "today", or an em dash. */
object WidgetDayCaption {
    const val EMPTY = "—"

    fun text(
        isFuture: Boolean,
        isToday: Boolean,
        dayGross: Double,
        todayLabel: String,
    ): String = when {
        isToday -> todayLabel
        isFuture -> EMPTY
        dayGross > 0.0 -> WidgetStatsFormatter.formatGrossUsd(dayGross)
        else -> EMPTY
    }

    fun usesEmptyColor(
        isFuture: Boolean,
        isToday: Boolean,
        dayGross: Double,
    ): Boolean = text(isFuture, isToday, dayGross, todayLabel = "x") == EMPTY
}
