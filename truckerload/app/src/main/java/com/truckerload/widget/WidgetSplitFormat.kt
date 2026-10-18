package com.truckerload.widget

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "12 – 18 окт" when the week stays in one month, otherwise both months. */
internal fun formatSplitWeekRange(
    start: LocalDate,
    end: LocalDate,
    locale: Locale = Locale.getDefault(),
): String {
    val month = DateTimeFormatter.ofPattern("LLL", locale)
    fun label(date: LocalDate): String =
        date.format(month).replace(".", "").lowercase(locale)
    return if (start.month == end.month && start.year == end.year) {
        "${start.dayOfMonth} – ${end.dayOfMonth} ${label(end)}"
    } else {
        "${start.dayOfMonth} ${label(start)} – ${end.dayOfMonth} ${label(end)}"
    }
}
