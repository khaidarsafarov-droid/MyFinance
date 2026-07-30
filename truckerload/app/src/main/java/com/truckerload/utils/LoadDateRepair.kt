package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import java.util.Calendar

/**
 * Corrects load.date / reporting week when Relay `MM/DD` times were anchored to the
 * wrong calendar year (e.g. Telegram history imported in 2026 with Pu-time `07/05`
 * becoming 2026-07-05 instead of the message year).
 */
object LoadDateRepair {

    /**
     * Prefer [anchorYearHint] (Telegram message year). Else use [Load.parsedAt] year
     * when the stored date's year disagrees with stop times re-parsed under that year.
     */
    fun repair(load: Load, anchorYearHint: Int? = null): Load {
        val anchorYear = anchorYearHint
            ?: yearFromMillis(load.parsedAt)
            ?: return load
        val puDates = load.stops
            .filter { it.type == StopType.PU }
            .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, anchorYear) }
        val delDates = load.stops
            .filter { it.type == StopType.DEL }
            .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, anchorYear) }
        if (puDates.isEmpty() && delDates.isEmpty()) return load

        val repairedDate = puDates.minOrNull()
            ?: delDates.minOrNull()
            ?: load.date
        if (repairedDate == load.date && load.weekNumber > 0 && load.year > 0) {
            // Date already matches; still ensure reporting week is consistent.
            val (w, y) = getLoadReportingWeek(load)
            if (w == load.weekNumber && y == load.year) return load
        }
        return load.copy(date = repairedDate).withReportingWeek().withRouteMetrics()
    }

    /**
     * Year to use for Relay `MM/DD` when only an anchor year is known.
     *
     * Picks among [anchorYear - 1], [anchorYear], [anchorYear + 1] the civil date
     * whose noon is closest to [nowMillis]. This keeps near-term bookings
     * (e.g. PU in 3 weeks) in the live year while still mapping winter history
     * viewed in spring to the previous year.
     */
    fun resolveRelayYear(
        month: Int,
        day: Int,
        anchorYear: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        if (month !in 1..12 || day !in 1..31) return anchorYear
        fun noonMillis(year: Int): Long? {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // FIX: reject impossible calendar days that rolled into the next month
            if (cal.get(Calendar.MONTH) != month - 1 || cal.get(Calendar.DAY_OF_MONTH) != day) {
                return null
            }
            return cal.timeInMillis
        }
        // FIX: nearest of three years — preserves live bookings beyond 14 days
        return listOf(anchorYear - 1, anchorYear, anchorYear + 1)
            .mapNotNull { year -> noonMillis(year)?.let { year to it } }
            .minByOrNull { (_, millis) -> kotlin.math.abs(millis - nowMillis) }
            ?.first
            ?: anchorYear
    }

    private fun yearFromMillis(millis: Long): Int? {
        if (millis <= 0L) return null
        return Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)
    }
}
