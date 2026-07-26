package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Неделя водителя: воскресенье — первый день (Sun–Sat). */
private fun truckingWeekCalendar(): Calendar =
    Calendar.getInstance(Locale.US).apply {
        firstDayOfWeek = Calendar.SUNDAY
        minimalDaysInFirstWeek = 1
    }

private fun weekSortKey(weekNumber: Int, year: Int): Long = year * 100L + weekNumber

private fun isWeekAfter(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean =
    weekSortKey(a.first, a.second) > weekSortKey(b.first, b.second)

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

/** Get (weekNumber, year) from timestamp (millis) in device default timezone. */
fun getWeekNumberAndYearFromTimestamp(millis: Long): Pair<Int, Int> {
    val cal = truckingWeekCalendar().apply { timeInMillis = millis }
    return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
}

/**
 * Returns (weekStartDate "yyyy-MM-dd", weekEndDate "yyyy-MM-dd", weekLabel "Sun DD – Sat DD, YYYY") for the given week number and year.
 */
fun getWeekRange(weekNumber: Int, year: Int): Triple<String, String, String> {
    val cal = truckingWeekCalendar()
    cal.clear()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.WEEK_OF_YEAR, weekNumber)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    val startYear = cal.get(Calendar.YEAR)
    val startMonth = cal.get(Calendar.MONTH) + 1
    val startDay = cal.get(Calendar.DAY_OF_MONTH)
    val startStr = "%04d-%02d-%02d".format(startYear, startMonth, startDay)
    cal.add(Calendar.DAY_OF_YEAR, 6)
    val endYear = cal.get(Calendar.YEAR)
    val endMonth = cal.get(Calendar.MONTH) + 1
    val endDay = cal.get(Calendar.DAY_OF_MONTH)
    val endStr = "%04d-%02d-%02d".format(endYear, endMonth, endDay)
    val shortMonths = DateFormatSymbols(Locale.getDefault()).shortMonths
    val startMonthLabel = shortMonths.getOrNull((startMonth - 1).coerceIn(0, 11)).orEmpty().replace(".", "")
    val endMonthLabel = shortMonths.getOrNull((endMonth - 1).coerceIn(0, 11)).orEmpty().replace(".", "")
    val label = "$startMonthLabel $startDay – $endMonthLabel $endDay, $startYear"
    return Triple(startStr, endStr, label)
}

fun getCurrentWeekNumberAndYear(): Pair<Int, Int> {
    val cal = truckingWeekCalendar()
    return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
}

/** Shift trucking week by [deltaWeeks] (negative = previous). */
fun shiftWeekNumberAndYear(weekNumber: Int, year: Int, deltaWeeks: Int): Pair<Int, Int> {
    val cal = truckingWeekCalendar()
    cal.clear()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.WEEK_OF_YEAR, weekNumber)
    cal.add(Calendar.WEEK_OF_YEAR, deltaWeeks)
    return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
}

fun getPreviousWeekNumberAndYear(): Pair<Int, Int> {
    val cal = truckingWeekCalendar()
    cal.add(Calendar.WEEK_OF_YEAR, -1)
    return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
}

/** Current calendar week start and end as "YYYY-MM-DD" (device local). Use for filtering loads by date. */
fun getCurrentWeekStartAndEnd(): Pair<String, String> {
    val (week, year) = getCurrentWeekNumberAndYear()
    val (start, end, _) = getWeekRange(week, year)
    return Pair(start, end)
}

/** Days elapsed in current trucking week (Sun=1 … today). */
fun getDaysElapsedInCurrentWeek(): Int {
    val cal = truckingWeekCalendar()
    return cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 1
}

/** Days remaining in current trucking week including today (today … Sat). */
fun getDaysRemainingInCurrentWeek(): Int {
    val cal = truckingWeekCalendar()
    return Calendar.SATURDAY - cal.get(Calendar.DAY_OF_WEEK) + 1
}

/** Alias for widget / UI copy. */
fun getDaysLeftInWeek(): Int = getDaysRemainingInCurrentWeek()

/** Total days in trucking week (always 7). */
fun getDaysInTruckingWeek(): Int = 7

/** Days active in week (Sun…today inclusive), min 1. Past weeks → 7. */
fun getDaysActiveForWeek(weekNumber: Int, year: Int): Int {
    val (currentWeek, currentYear) = getCurrentWeekNumberAndYear()
    if (weekNumber != currentWeek || year != currentYear) return getDaysInTruckingWeek()
    return getDaysElapsedInCurrentWeek().coerceAtLeast(1)
}

/** Days remaining in week (today…Sat inclusive), min 1 for math. Past weeks → 1. */
fun getDaysRemainingForWeek(weekNumber: Int, year: Int): Int {
    val (currentWeek, currentYear) = getCurrentWeekNumberAndYear()
    if (weekNumber != currentWeek || year != currentYear) return 1
    return getDaysRemainingInCurrentWeek().coerceAtLeast(1)
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

/** Вчера в формате YYYY-MM-DD. */
fun getYesterdayDate(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    return "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

/** Прошлая неделя: (startDate, endDate) в формате YYYY-MM-DD. */
fun getLastWeekStartAndEnd(): Pair<String, String> {
    val (week, year) = getPreviousWeekNumberAndYear()
    val (start, end, _) = getWeekRange(week, year)
    return Pair(start, end)
}

/** Дата первого Pick Up (PU) или load.date. */
fun getPickUpDate(load: Load): String? {
    val yearHint = load.date.take(4).toIntOrNull()
    val fromStops = load.stops
        .filter { it.type == StopType.PU }
        .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, yearHint) }
        .minOrNull()
    return fromStops ?: load.date.takeIf { it.length >= 10 }
}

/** Дата последней доставки (DEL), если есть. */
fun getDeliveryDate(load: Load): String? {
    val yearHint = load.date.take(4).toIntOrNull()
    return load.stops
        .filter { it.type == StopType.DEL }
        .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, yearHint) }
        .maxOrNull()
}

/**
 * Неделя отчёта для груза: по дате PU (воскресенье = новая неделя).
 * Если доставка попадает в более позднюю неделю (напр. PU суббота, DEL воскресенье) — неделя доставки.
 */
fun getLoadReportingWeek(load: Load): Pair<Int, Int> {
    val puDate = getPickUpDate(load)
    val delDate = getDeliveryDate(load)
    val puWeek = getWeekNumberAndYearFromDate(puDate)
    if (delDate == null) return puWeek
    val delWeek = getWeekNumberAndYearFromDate(delDate)
    return if (isWeekAfter(delWeek, puWeek)) delWeek else puWeek
}

fun isLoadInWeek(load: Load, weekNumber: Int, year: Int): Boolean {
    // Prefer persisted reporting week (matches Room SQL / Stats). Recompute only when unset.
    if (load.weekNumber > 0 && load.year > 0) {
        return load.weekNumber == weekNumber && load.year == year
    }
    val (w, y) = getLoadReportingWeek(load)
    return w == weekNumber && y == year
}

fun Load.withReportingWeek(): Load {
    val (weekNumber, year) = getLoadReportingWeek(this)
    val puDate = getPickUpDate(this)
    return copy(
        weekNumber = weekNumber,
        year = year,
        date = puDate ?: date
    )
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

/**
 * Парсит дату из scheduledTime (YYYY-MM-DD HH:mm, DD.MM.YYYY, Relay `MM/DD HH:mm TZ`).
 * Возвращает YYYY-MM-DD или null. [defaultYear] used for US `MM/DD` Relay times without a year.
 */
fun parseDateFromScheduledTime(s: String, defaultYear: Int? = null): String? {
    if (s.isBlank()) return null
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
        val anchor = defaultYear ?: Calendar.getInstance().get(Calendar.YEAR)
        val year = LoadDateRepair.resolveRelayYear(month, day, anchor)
        if (month in 1..12 && day in 1..31 && year in 1970..2100) {
            return "%04d-%02d-%02d".format(year, month, day)
        }
        return null
    }
    return parseDateFromQuery(t)
}

/** Безопасный разбор YYYY-MM-DD; защита от NumberFormatException на битых датах в БД. */
internal fun parseIsoDateParts(dateStr: String): Triple<Int, Int, Int>? {
    val parts = dateStr.split("-")
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

/** Parse stop scheduledTime to epoch millis. Supports YYYY-MM-DD HH:mm and DD.MM.YYYY HH:mm. */
fun parseScheduledTimeToMillis(scheduledTime: String): Long? {
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
        val year = Calendar.getInstance(Locale.US).get(Calendar.YEAR)
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    val dateOnly = parseDateFromScheduledTime(t) ?: return null
    return dateStringToStartOfDayMillis(dateOnly)
}

private val US_RELAY_TIME = Regex("""^(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})(?:\s+([A-Z]{2,4}))?\s*$""")

/** First PU stop datetime (millis) or load.date at start of day. */
fun getFirstPickUpMillis(load: Load): Long? {
    val fromStops = load.stops
        .filter { it.type == StopType.PU }
        .mapNotNull { parseScheduledTimeToMillis(it.scheduledTime) }
        .minOrNull()
    if (fromStops != null) return fromStops
    return load.date.takeIf { it.length >= 10 }?.let { dateStringToStartOfDayMillis(it) }
}

/** Last DEL stop datetime (millis) or load.date at start of day. */
fun getLastDeliveryMillis(load: Load): Long? {
    val fromStops = load.stops
        .filter { it.type == StopType.DEL }
        .mapNotNull { parseScheduledTimeToMillis(it.scheduledTime) }
        .maxOrNull()
    if (fromStops != null) return fromStops
    return load.date.takeIf { it.length >= 10 }?.let { dateStringToStartOfDayMillis(it) }
}

/** Для груза возвращает множество дат (YYYY-MM-DD), когда груз активен: от первой до последней остановки включительно. */
fun getLoadDateRange(load: Load): Set<String> {
    val dates = mutableSetOf<String>()
    canonicalDateString(load.date)?.let { dates.add(it) }
    val yearHint = load.date.take(4).toIntOrNull()
    val stopDates = load.stops.mapNotNull { parseDateFromScheduledTime(it.scheduledTime, yearHint) }
    if (stopDates.isNotEmpty()) {
        val sorted = stopDates.sorted()
        val start = sorted.first()
        val end = load.actualFinishDate?.takeIf { it.length >= 10 }?.take(10)
            ?.takeIf { it >= start }
            ?: sorted.last()
        val p1 = start.split("-").mapNotNull { it.toIntOrNull() }
        val p2 = end.split("-").mapNotNull { it.toIntOrNull() }
        if (p1.size == 3 && p2.size == 3) {
            val cal = Calendar.getInstance()
            cal.set(p1[0], p1[1] - 1, p1[2])
            val endCal = Calendar.getInstance()
            endCal.set(p2[0], p2[1] - 1, p2[2])
            while (!cal.after(endCal)) {
                dates.add("%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    } else {
        load.actualFinishDate?.takeIf { it.length >= 10 }?.let { dates.add(it.take(10)) }
    }
    return dates
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

/** Short week label like "24 Feb — 2 Mar" using locale firstDayOfWeek. */
fun getWeekLabelShort(weekNumber: Int, year: Int): String {
    val (start, end, _) = getWeekRange(weekNumber, year)
    val startParts = start.split("-")
    val endParts = end.split("-")
    if (startParts.size != 3 || endParts.size != 3) return "Week $weekNumber"
    val monthNamesShort = DateFormatSymbols(Locale.getDefault()).shortMonths
    val startD = startParts[2].toIntOrNull() ?: 0
    val startM = startParts[1].toIntOrNull() ?: 0
    val endD = endParts[2].toIntOrNull() ?: 0
    val endM = endParts[1].toIntOrNull() ?: 0
    val startLabel = monthNamesShort.getOrNull((startM - 1).coerceIn(0, 11)).orEmpty().replace(".", "").lowercase(Locale.getDefault())
    val endLabel = monthNamesShort.getOrNull((endM - 1).coerceIn(0, 11)).orEmpty().replace(".", "").lowercase(Locale.getDefault())
    return "$startD $startLabel — $endD $endLabel"
}

/** Enumerates `count` consecutive trucking weeks ending at the current week (oldest first). */
fun enumerateRecentWeekSlots(count: Int): List<Pair<Int, Int>> {
    val cal = truckingWeekCalendar()
    cal.add(Calendar.WEEK_OF_YEAR, -(count - 1).coerceAtLeast(0))
    return buildList {
        repeat(count.coerceAtLeast(1)) {
            add(cal.get(Calendar.WEEK_OF_YEAR) to cal.get(Calendar.YEAR))
            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }
    }
}

/** Weeks W1..throughWeek for the given calendar year. */
fun enumerateYearWeekSlots(year: Int, throughWeek: Int): List<Pair<Int, Int>> =
    (1..throughWeek.coerceAtLeast(1)).map { it to year }

/** Недели, пересекающие месяц. Возвращает список (weekNumber, year) для календаря. Неделя с воскресенья. */
fun getWeeksInMonth(year: Int, month: Int): List<Pair<Int, Int>> {
    val cal = truckingWeekCalendar()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val firstDayOfWeek = Calendar.SUNDAY
    var diff = (cal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, -diff)
    val result = mutableListOf<Pair<Int, Int>>()
    val targetMonth = month - 1
    val lastDay = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, targetMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
    while (cal.get(Calendar.YEAR) < year || cal.get(Calendar.MONTH) < targetMonth ||
        (cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.DAY_OF_MONTH) <= lastDay)) {
        val endOfWeek = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        if (cal.get(Calendar.MONTH) == targetMonth || endOfWeek.get(Calendar.MONTH) == targetMonth) {
            result.add(Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR)))
        }
        cal.add(Calendar.DAY_OF_YEAR, 7)
        if (cal.get(Calendar.YEAR) > year) break
        if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) > targetMonth) break
    }
    return result.distinct()
}

/** Возвращает (startDate, endDate) для месяца. */
fun getMonthRange(month: Int, year: Int): Pair<String, String> {
    val cal = java.util.Calendar.getInstance()
    cal.set(year, month - 1, 1)
    val start = "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, 1)
    val lastDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val end = "%04d-%02d-%02d".format(year, month, lastDay)
    return Pair(start, end)
}

/** Квартал (1-based): Q1=Jan-Mar, Q2=Apr-Jun, Q3=Jul-Sep, Q4=Oct-Dec. */
fun getQuarterRange(quarter: Int, year: Int): Pair<String, String> {
    val (startM, endM) = when (quarter) {
        1 -> 1 to 3
        2 -> 4 to 6
        3 -> 7 to 9
        4 -> 10 to 12
        else -> 1 to 3
    }
    val cal = java.util.Calendar.getInstance()
    cal.set(year, endM - 1, 1)
    val lastDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val start = "%04d-%02d-01".format(year, startM)
    val end = "%04d-%02d-%02d".format(year, endM, lastDay)
    return Pair(start, end)
}

/** (quarter, year) из месяца. */
fun getQuarterFromMonth(month: Int): Int = ((month - 1) / 3) + 1

/** Предыдущий месяц. */
fun getPreviousMonth(month: Int, year: Int): Pair<Int, Int> {
    if (month <= 1) return Pair(12, year - 1)
    return Pair(month - 1, year)
}

/** Предыдущий квартал. */
fun getPreviousQuarter(quarter: Int, year: Int): Pair<Int, Int> {
    if (quarter <= 1) return Pair(4, year - 1)
    return Pair(quarter - 1, year)
}

/**
 * Parse "YYYY-MM-DD" to (weekNumber, year). On error returns current week/year.
 */
fun getWeekNumberAndYearFromDate(dateStr: String?): Pair<Int, Int> {
    if (dateStr.isNullOrBlank() || dateStr.length < 10) return getCurrentWeekNumberAndYear()
    return try {
        val parts = dateStr.split("-")
        if (parts.size != 3) return getCurrentWeekNumberAndYear()
        val y = parts[0].toIntOrNull() ?: return getCurrentWeekNumberAndYear()
        val m = parts[1].toIntOrNull()?.minus(1) ?: return getCurrentWeekNumberAndYear()
        val d = parts[2].toIntOrNull() ?: return getCurrentWeekNumberAndYear()
        val cal = truckingWeekCalendar()
        cal.set(y, m, d)
        Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
    } catch (e: Exception) {
        getCurrentWeekNumberAndYear()
    }
}
