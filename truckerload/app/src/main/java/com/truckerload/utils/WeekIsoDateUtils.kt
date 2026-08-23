package com.truckerload.utils

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Format timestamp (millis) to display date DD.MM.YYYY in device local timezone. */
fun formatDateForDisplay(millis: Long): String {
    val cal = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = millis }
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val m = cal.get(Calendar.MONTH) + 1
    val y = cal.get(Calendar.YEAR)
    return "%02d.%02d.%04d".format(d, m, y)
}

/** Format timestamp (millis) to display string DD.MM.YYYY HH:mm in device local timezone. */
fun formatDateTimeForDisplay(millis: Long): String {
    val cal = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = millis }
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val m = cal.get(Calendar.MONTH) + 1
    val y = cal.get(Calendar.YEAR)
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val min = cal.get(Calendar.MINUTE)
    return "%02d.%02d.%04d %02d:%02d".format(d, m, y, h, min)
}

/** Convert Telegram message date (Unix seconds) to "YYYY-MM-DD". Used to save loads by message date. */
fun formatDateFromUnixSeconds(unixSeconds: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = unixSeconds * 1000L
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

/** Formats report date range: "01 March — 31 March 2026" in current locale. */
fun formatDateRangeForReport(startDate: String, endDate: String): String {
    val monthNames = DateFormatSymbols(Locale.getDefault()).months
    fun parse(s: String): Triple<Int, Int, Int>? {
        val p = s.split("-")
        if (p.size != 3) return null
        val y = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        val d = p[2].toIntOrNull() ?: return null
        return Triple(d, m, y)
    }
    val start = parse(startDate) ?: return "$startDate — $endDate"
    val end = parse(endDate) ?: return "$startDate — $endDate"
    val (d1, m1, y1) = start
    val (d2, m2, y2) = end
    val sm = monthNames.getOrNull((m1 - 1).coerceIn(0, 11)).orEmpty()
    val em = monthNames.getOrNull((m2 - 1).coerceIn(0, 11)).orEmpty()
    return "%02d %s — %02d %s %d".format(d1, sm, d2, em, y2)
}

/** Normalize user-facing date text to YYYY-MM-DD when possible. */
fun canonicalDateString(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
        return trimmed.take(10)
    }
    return parseDateFromQuery(trimmed)
}

/** Безопасный разбор YYYY-MM-DD; защита от NumberFormatException на битых датах в БД. */
internal fun parseIsoDateParts(dateStr: String): Triple<Int, Int, Int>? {
    // Accept "YYYY-MM-DD" and "YYYY-MM-DD HH:mm" (date prefix only).
    val parts = dateStr.trim().take(10).split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31 || year !in 1970..2100) return null
    return Triple(year, month, day)
}

/** Epoch millis for YYYY-MM-DD at start of day (local). Null if parse fails. */
fun dateStringToStartOfDayMillis(dateStr: String): Long? {
    val (year, month, day) = parseIsoDateParts(dateStr) ?: return null
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.set(year, month - 1, day, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Epoch millis for YYYY-MM-DD at end of day (local). Null if parse fails. */
fun dateStringToEndOfDayMillis(dateStr: String): Long? {
    val (year, month, day) = parseIsoDateParts(dateStr) ?: return null
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.set(year, month - 1, day, 23, 59, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

/**
 * Material3 [androidx.compose.material3.DatePicker] stores selected days as
 * UTC midnight. Convert YYYY-MM-DD → that UTC millis for [rememberDatePickerState].
 */
fun dateStringToUtcDatePickerMillis(dateStr: String): Long? {
    val (year, month, day) = parseIsoDateParts(dateStr) ?: return null
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
    cal.clear()
    cal.set(year, month - 1, day, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/**
 * Inverse of [dateStringToUtcDatePickerMillis]: UTC midnight millis → YYYY-MM-DD
 * of the calendar day the user tapped in the DatePicker.
 */
fun utcDatePickerMillisToDateString(utcMillis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
    cal.timeInMillis = utcMillis
    return "%04d-%02d-%02d".format(
        Locale.US,
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}

/** Local-calendar day of [millis] as YYYY-MM-DD. */
fun formatIsoDate(millis: Long): String {
    val cal = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = millis }
    return "%04d-%02d-%02d".format(
        Locale.US,
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}

/**
 * Apply a Material3 DatePicker UTC-midnight selection onto [keepTimeFromMillis],
 * keeping the local time-of-day. Avoids the previous-day shift in US timezones.
 */
fun applyUtcDatePickerDay(utcMidnightMillis: Long, keepTimeFromMillis: Long): Long {
    val parts = parseIsoDateParts(utcDatePickerMillisToDateString(utcMidnightMillis))
        ?: return keepTimeFromMillis
    return Calendar.getInstance(Locale.getDefault()).apply {
        timeInMillis = keepTimeFromMillis
        set(Calendar.YEAR, parts.first)
        set(Calendar.MONTH, parts.second - 1)
        set(Calendar.DAY_OF_MONTH, parts.third)
    }.timeInMillis
}

/** Парсит дату из строки (DD.MM.YYYY, DD.MM.YY, YYYY-MM-DD). Возвращает YYYY-MM-DD или null. */
fun parseDateFromQuery(query: String): String? {
    val trimmed = query.trim()
    if (trimmed.length < 8) return null
    return when {
        // YYYY-MM-DD
        trimmed.contains("-") && trimmed.split("-").size == 3 -> {
            val parts = trimmed.split("-")
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (y in 2000..2100 && m in 1..12 && d in 1..31) "%04d-%02d-%02d".format(y, m, d) else null
        }
        // DD.MM.YYYY или DD.MM.YY
        trimmed.contains(".") && trimmed.split(".").size == 3 -> {
            val parts = trimmed.split(".")
            val d = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            var y = parts[2].toIntOrNull() ?: return null
            if (y in 0..99) y += 2000
            if (y in 2000..2100 && m in 1..12 && d in 1..31) "%04d-%02d-%02d".format(y, m, d) else null
        }
        else -> null
    }
}
