package com.truckerload.presentation.screens.home

import java.text.DateFormatSymbols
import java.util.Locale

/** Localized "September 2026" style label for the Home month filter. */
internal fun formatHomeMonthLabel(month: Int, year: Int, locale: Locale = Locale.getDefault()): String {
    val name = DateFormatSymbols(locale).months
        .getOrNull((month - 1).coerceIn(0, 11))
        .orEmpty()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    return "$name $year"
}
