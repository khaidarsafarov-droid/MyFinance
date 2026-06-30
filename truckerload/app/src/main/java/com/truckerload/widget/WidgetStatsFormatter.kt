package com.truckerload.widget

import java.util.Locale

object WidgetStatsFormatter {

    fun formatGross(value: Double): String =
        String.format(Locale.US, "%,.0f$", value)

    /** USD with leading dollar sign for home-screen widget. */
    fun formatGrossUsd(value: Double): String =
        String.format(Locale.US, "$%,.0f", value)

    fun formatCpm(value: Double): String =
        String.format(Locale.US, "%.2f$", value)

    fun formatMiles(value: Double): String =
        String.format(Locale.US, "%,.0f", value) + " миль"

    /** Compact value for bento cell (no unit suffix — label carries context). */
    fun formatMilesShort(value: Double): String =
        String.format(Locale.US, "%,.0f", value)

    fun formatProgressPercent(progress: Float): String =
        String.format(Locale.US, "%.1f%%", progress.coerceIn(0f, 100f))

    fun formatRpmPerMile(rpm: Double): String =
        String.format(Locale.US, "$%.2f/mi", rpm)

    fun avgRpm(totalRate: Double, totalMiles: Double): Double =
        if (totalMiles > 0) totalRate / totalMiles else 0.0

    /** Compact pace for widget inline row, e.g. $607/d */
    fun formatDailyPaceShort(value: Double): String {
        if (value <= 0.0) return "$0/d"
        val hasFraction = kotlin.math.abs(value - value.toLong()) > 0.009
        return if (hasFraction) {
            String.format(Locale.US, "$%,.2f/d", value)
        } else {
            String.format(Locale.US, "$%,.0f/d", value)
        }
    }

    /** Pace as gross per active day, e.g. $607/день or $607.50/день */
    fun formatDailyPace(value: Double): String {
        if (value <= 0.0) return "$0/день"
        val hasFraction = kotlin.math.abs(value - value.toLong()) > 0.009
        return if (hasFraction) {
            String.format(Locale.US, "$%,.2f/день", value)
        } else {
            String.format(Locale.US, "$%,.0f/день", value)
        }
    }
}
