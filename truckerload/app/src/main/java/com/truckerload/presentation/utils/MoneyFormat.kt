package com.truckerload.presentation.utils

import java.util.Locale

/** Единое форматирование сумм и чисел (US: запятая в тысячах, точка в десятичных). */
object MoneyFormat {
    private val locale = Locale.US

    fun formatCurrency(value: Double, decimals: Int = 0): String =
        if (decimals == 0) {
            String.format(locale, "$%,.0f", value)
        } else {
            String.format(locale, "$%,.${decimals}f", value)
        }

    fun formatNumber(value: Double, decimals: Int = 0): String =
        if (decimals == 0) {
            String.format(locale, "%,.0f", value)
        } else {
            String.format(locale, "%,.${decimals}f", value)
        }

    fun formatRpm(value: Double): String =
        String.format(locale, "$%.2f/mi", value)

    fun formatRpmShort(value: Double): String =
        String.format(locale, "$%.2f", value)

    fun formatMiles(value: Double, decimals: Int = 0): String =
        "${formatNumber(value, decimals)} mi"
}
