package com.truckerload.utils

import com.truckerload.domain.model.Load
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

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
    val cal = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = millis }
    return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
}

/**
 * Returns (weekStartDate "yyyy-MM-dd", weekEndDate "yyyy-MM-dd", weekLabel "Mon DD – Mon DD, YYYY") for the given week number and year.
 */
fun getWeekRange(weekNumber: Int, year: Int): Triple<String, String, String> {
    val cal = Calendar.getInstance()
    cal.clear()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.WEEK_OF_YEAR, weekNumber)
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
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
    val cal = Calendar.getInstance()
    return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
}

/** Current calendar week start and end as "YYYY-MM-DD" (device local). Use for filtering loads by date. */
fun getCurrentWeekStartAndEnd(): Pair<String, String> {
    val (week, year) = getCurrentWeekNumberAndYear()
    val (start, end, _) = getWeekRange(week, year)
    return Pair(start, end)
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
    val cal = Calendar.getInstance()
    cal.add(Calendar.WEEK_OF_YEAR, -1)
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val start = "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    cal.add(Calendar.DAY_OF_YEAR, 6)
    val end = "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    return Pair(start, end)
}

/** Парсит дату из scheduledTime (YYYY-MM-DD HH:mm, DD.MM.YYYY, etc). Возвращает YYYY-MM-DD или null. */
fun parseDateFromScheduledTime(s: String): String? {
    if (s.isBlank()) return null
    val t = s.trim()
    if (t.length >= 10 && t[4] == '-' && t[7] == '-') {
        val sub = t.substring(0, 10)
        if (sub.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return sub
    }
    return parseDateFromQuery(t)
}

/** Для груза возвращает множество дат (YYYY-MM-DD), когда груз активен: от первой до последней остановки включительно. */
fun getLoadDateRange(load: Load): Set<String> {
    val dates = mutableSetOf<String>()
    if (load.date.length >= 10) dates.add(load.date)
    val stopDates = load.stops.mapNotNull { parseDateFromScheduledTime(it.scheduledTime) }
    if (stopDates.isNotEmpty()) {
        val sorted = stopDates.sorted()
        val start = sorted.first()
        val end = sorted.last()
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

/** Недели, пересекающие месяц. Возвращает список (weekNumber, year) для календаря. Учитывает firstDayOfWeek. */
fun getWeeksInMonth(year: Int, month: Int): List<Pair<Int, Int>> {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val firstDayOfWeek = cal.firstDayOfWeek
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
        val cal = Calendar.getInstance()
        cal.set(y, m, d)
        Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
    } catch (_: Exception) {
        getCurrentWeekNumberAndYear()
    }
}
