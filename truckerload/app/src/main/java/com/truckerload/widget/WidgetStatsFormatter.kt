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
        String.format(Locale.US, "%,.0f", value) + " mi"

    /** Compact value for bento cell (no unit suffix — label carries context). */
    fun formatMilesShort(value: Double): String =
        String.format(Locale.US, "%,.0f", value)

    fun formatProgressPercent(progress: Float): String =
        String.format(Locale.US, "%.1f%%", progress.coerceIn(0f, 100f))

    fun formatRpmPerMile(context: android.content.Context, rpm: Double): String =
        context.getString(com.truckerload.R.string.rpm_per_mile_format, rpm)

    @Deprecated("Use formatRpmPerMile(context, rpm) for localized unit")
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

    /** Pace as gross per active day, e.g. $607/day or $607.50/day */
    fun formatDailyPace(value: Double): String {
        if (value <= 0.0) return "$0/day"
        val hasFraction = kotlin.math.abs(value - value.toLong()) > 0.009
        return if (hasFraction) {
            String.format(Locale.US, "$%,.2f/day", value)
        } else {
            String.format(Locale.US, "$%,.0f/day", value)
        }
    }

    /**
     * RemoteViews has no auto-size on API 24–25. Shrink the hero amount so
     * `$50,000` still fits beside the ring without changing widget bounds.
     */
    fun amountSp(formattedUsd: String, defaultSp: Float): Float = when {
        formattedUsd.length >= 10 -> (defaultSp * 0.55f).coerceAtLeast(12f)
        formattedUsd.length >= 8 -> (defaultSp * 0.68f).coerceAtLeast(13f)
        formattedUsd.length >= 7 -> (defaultSp * 0.80f).coerceAtLeast(14f)
        else -> defaultSp
    }

    /** Percent inside the ring (`24.8%` / `100.0%`) — always a short string. */
    fun percentSp(defaultSp: Float): Float = defaultSp.coerceIn(8f, 16f)
}
