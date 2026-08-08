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

    /** Epoch before this is treated as unset/sentinel (tests often use `1L`). */
    private const val MIN_SANE_REFERENCE_MS = 946_684_800_000L // 2000-01-01 UTC

    /**
     * Prefer [anchorYearHint] (Telegram message year). Else use [Load.parsedAt] year
     * when the stored date's year disagrees with stop times re-parsed under that year.
     *
     * Year resolution is anchored to [Load.parsedAt] (or the message time), **not**
     * wall-clock "now", so a load dated `08/20` that correctly resolved to the previous
     * year in July is not flipped to the current year once August arrives.
     */
    fun repair(
        load: Load,
        anchorYearHint: Int? = null,
        referenceMillis: Long? = null,
    ): Load {
        val refMillis = saneReferenceMillis(referenceMillis)
            ?: saneReferenceMillis(load.parsedAt)
            ?: System.currentTimeMillis()
        val anchorYear = anchorYearHint
            ?: yearFromMillis(refMillis)
            ?: return load
        val puDates = load.stops
            .filter { it.type == StopType.PU }
            .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, anchorYear, refMillis) }
        val delDates = load.stops
            .filter { it.type == StopType.DEL }
            .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, anchorYear, refMillis) }
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
     * Year to use for Relay `MM/DD` when only an anchor timestamp is known.
     *
     * [referenceMillis] should be the Telegram message time / [Load.parsedAt] when
     * available. Using wall-clock "now" on every hydrate/repair makes year labels
     * drift as the calendar approaches the MM/DD (e.g. July→August).
     */
    fun resolveRelayYear(
        month: Int,
        day: Int,
        anchorYear: Int,
        referenceMillis: Long = System.currentTimeMillis(),
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
        // Near-term bookings (about two weeks after the message/parse anchor) keep the
        // anchor year; farther "future" MM/DD from Telegram history is almost always
        // the previous calendar year.
        val bookingHorizonMs = 14L * 24 * 60 * 60 * 1000
        return if (candidate.timeInMillis - referenceMillis > bookingHorizonMs) {
            anchorYear - 1
        } else {
            anchorYear
        }
    }

    private fun yearFromMillis(millis: Long): Int? {
        val sane = saneReferenceMillis(millis) ?: return null
        return Calendar.getInstance().apply { timeInMillis = sane }.get(Calendar.YEAR)
    }

    private fun saneReferenceMillis(millis: Long?): Long? =
        millis?.takeIf { it >= MIN_SANE_REFERENCE_MS }
}
