package com.truckerload.presentation.utils

import com.truckerload.domain.money.Money
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import java.util.Locale
import kotlin.math.abs

/** Единое форматирование сумм и чисел (US: запятая в тысячах, точка в десятичных). */
object MoneyFormat {
    private val locale = Locale.US

    fun formatCurrency(value: Double, decimals: Int = 0): String =
        if (decimals == 0) {
            String.format(locale, "$%,.0f", value)
        } else {
            String.format(locale, "$%,.${decimals}f", value)
        }

    /** Formats [Money] from integer cents. Default is two decimal places. */
    fun formatCurrency(value: Money, decimals: Int = 2): String {
        val negative = value.cents < 0
        val absCents = abs(value.cents)
        val sign = if (negative) "-" else ""
        if (decimals <= 0) {
            val dollars = (absCents + 50L) / 100L
            return sign + String.format(locale, "$%,d", dollars)
        }
        if (decimals == 1) {
            val tenths = (absCents + 5L) / 10L
            val dollars = tenths / 10L
            val fraction = (tenths % 10L).toString()
            return sign + String.format(locale, "$%,d", dollars) + "." + fraction
        }
        val dollars = absCents / 100L
        val centsPart = (absCents % 100L).toString().padStart(2, '0')
        val extra = if (decimals > 2) "0".repeat(decimals - 2) else ""
        return sign + String.format(locale, "$%,d", dollars) + "." + centsPart + extra
    }

    fun formatNumber(value: Double, decimals: Int = 0): String =
        if (decimals == 0) {
            String.format(locale, "%,.0f", value)
        } else {
            String.format(locale, "%,.${decimals}f", value)
        }

    fun formatRpm(value: Double): String {
        val shown = MeasurementUnits.displayRatePerDistance(value)
        val suffix = if (DisplayUnits.isMetric) "/km" else "/mi"
        return String.format(locale, "$%.2f$suffix", shown)
    }

    fun formatRpmShort(value: Double): String =
        String.format(locale, "$%.2f", MeasurementUnits.displayRatePerDistance(value))

    /** Distance number only. Miles mode matches [formatNumber] of the stored miles. */
    fun formatDistanceNumber(canonicalMiles: Double, decimals: Int = 0): String =
        formatNumber(MeasurementUnits.displayDistance(canonicalMiles), decimals)

    fun formatMiles(value: Double, decimals: Int = 0): String {
        val suffix = if (DisplayUnits.isMetric) "km" else "mi"
        return "${formatDistanceNumber(value, decimals)} $suffix"
    }

    fun formatVolumeNumber(canonicalGallons: Double, decimals: Int = 2): String =
        formatNumber(MeasurementUnits.displayVolume(canonicalGallons), decimals)
}
