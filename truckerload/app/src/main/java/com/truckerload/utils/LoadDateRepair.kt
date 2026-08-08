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

    /** Year to use for Relay `MM/DD` when only an anchor timestamp is known. */
    fun resolveRelayYear(
        month: Int,
        day: Int,
        anchorYear: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        if (month !in 1..12 || day !in 1..31) return anchorYear
        val candidate = Calendar.getInstance().apply {
            set(Calendar.YEAR, anchorYear)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Near-term Relay bookings (~1 week) keep the anchor year. Farther "future"
        // MM/DD from Telegram history is almost always the previous calendar year
        // (e.g. early August must not paint last August's 08/20–08/21 loads as this year).
        val bookingHorizonMs = 7L * 24 * 60 * 60 * 1000
        return if (candidate.timeInMillis - nowMillis > bookingHorizonMs) anchorYear - 1 else anchorYear
    }

    private fun yearFromMillis(millis: Long): Int? {
        if (millis <= 0L) return null
        return Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)
    }
}
