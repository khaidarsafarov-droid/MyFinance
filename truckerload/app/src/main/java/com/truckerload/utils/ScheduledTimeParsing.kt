package com.truckerload.utils

import java.util.Calendar
import java.util.Locale

/**
 * Парсит дату из scheduledTime (YYYY-MM-DD HH:mm, DD.MM.YYYY, Relay `MM/DD HH:mm TZ`).
 * Возвращает YYYY-MM-DD или null. [defaultYear] used for US `MM/DD` Relay times without a year.
 * [referenceMillis] anchors the "near-term booking" year decision (message/parsedAt time).
 * When [trustDefaultYear] is true and [defaultYear] is set (e.g. from [Load.date]), the year is
 * used as-is — do not re-apply the booking-horizon heuristic on every UI read.
 */
fun parseDateFromScheduledTime(
    s: String?,
    defaultYear: Int? = null,
    referenceMillis: Long? = null,
    trustDefaultYear: Boolean = false,
): String? {
    if (s.isNullOrBlank()) return null
    val t = s.trim()
    if (t.length >= 10 && t[4] == '-' && t[7] == '-') {
        val sub = t.substring(0, 10)
        if (sub.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return sub
    }
    // Relay US: "07/06 00:01 EDT" / "7/6 18:30 CDT"
    val us = Regex("""^(\d{1,2})/(\d{1,2})(?:\s|$)""").find(t)
    if (us != null) {
        val month = us.groupValues[1].toIntOrNull() ?: return null
        val day = us.groupValues[2].toIntOrNull() ?: return null
        val year = resolveRelayParseYear(month, day, defaultYear, referenceMillis, trustDefaultYear)
        if (month in 1..12 && day in 1..31 && year in 1970..2100) {
            return "%04d-%02d-%02d".format(year, month, day)
        }
        return null
    }
    return parseDateFromQuery(t)
}

/**
 * Parse stop scheduledTime to epoch millis.
 * Supports YYYY-MM-DD HH:mm, DD.MM.YYYY HH:mm, and Relay `MM/DD HH:mm TZ`.
 * [defaultYear] anchors yearless Relay times (same rules as [parseDateFromScheduledTime]).
 * [referenceMillis] anchors the near-term booking year decision (message/parsedAt time).
 */
fun parseScheduledTimeToMillis(
    scheduledTime: String,
    defaultYear: Int? = null,
    referenceMillis: Long? = null,
    trustDefaultYear: Boolean = false,
): Long? {
    if (scheduledTime.isBlank()) return null
    val t = scheduledTime.trim()
    val iso = Regex("""^(\d{4})-(\d{2})-(\d{2})\s+(\d{1,2}):(\d{2})""")
    iso.find(t)?.let { m ->
        val y = m.groupValues[1]
        val mo = m.groupValues[2]
        val d = m.groupValues[3]
        val h = m.groupValues[4]
        val mi = m.groupValues[5]
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.set(y.toInt(), mo.toInt() - 1, d.toInt(), h.toInt(), mi.toInt(), 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    val eu = Regex("""^(\d{1,2})\.(\d{1,2})\.(\d{2,4})\s+(\d{1,2}):(\d{2})""")
    eu.find(t)?.let { m ->
        val d = m.groupValues[1]
        val mo = m.groupValues[2]
        var y = m.groupValues[3].toInt()
        val h = m.groupValues[4]
        val mi = m.groupValues[5]
        if (y in 0..99) y += 2000
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.set(y, mo.toInt() - 1, d.toInt(), h.toInt(), mi.toInt(), 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    US_RELAY_TIME.find(t)?.let { m ->
        val month = m.groupValues[1].toInt()
        val day = m.groupValues[2].toInt()
        val hour = m.groupValues[3].toInt()
        val minute = m.groupValues[4].toInt()
        // FIX: align with parseDateFromScheduledTime — current-year alone skews PU→DEL by ~365d
        val year = resolveRelayParseYear(month, day, defaultYear, referenceMillis, trustDefaultYear)
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    val dateOnly = parseDateFromScheduledTime(
        t,
        defaultYear,
        referenceMillis,
        trustDefaultYear,
    ) ?: return null
    return dateStringToStartOfDayMillis(dateOnly)
}

private val US_RELAY_TIME = Regex("""^(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})(?:\s+([A-Z]{2,4}))?\s*$""")

/**
 * FIX: [defaultYear] without [referenceMillis] must not re-anchor MM/DD to wall-clock.
 * Booking-horizon math runs only when a real message/parsedAt timestamp is supplied.
 */
private fun resolveRelayParseYear(
    month: Int,
    day: Int,
    defaultYear: Int?,
    referenceMillis: Long?,
    trustDefaultYear: Boolean,
): Int {
    if (defaultYear != null && (trustDefaultYear || referenceMillis == null || referenceMillis <= 0L)) {
        return defaultYear
    }
    val anchor = defaultYear ?: Calendar.getInstance(Locale.US).get(Calendar.YEAR)
    return LoadDateRepair.resolveRelayYear(
        month = month,
        day = day,
        anchorYear = anchor,
        referenceMillis = referenceMillis?.takeIf { it > 0L } ?: System.currentTimeMillis(),
    )
}
