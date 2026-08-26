package com.truckerload.widget

/** Captions under Sun–Sat chips on the green cabin widget. */
object WidgetDayCaption {
    const val EMPTY = "—"

    fun text(
        selected: Boolean,
        isToday: Boolean,
        dayGross: Double,
        todayLabel: String,
    ): String = when {
        selected -> WidgetStatsFormatter.formatGrossUsd(dayGross)
        isToday -> todayLabel
        else -> EMPTY
    }
}
