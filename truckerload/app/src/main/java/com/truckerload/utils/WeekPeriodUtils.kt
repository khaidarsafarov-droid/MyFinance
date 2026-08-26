package com.truckerload.utils

import com.truckerload.domain.week.WeekStartDay
import com.truckerload.domain.week.WeekStartRuntime
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

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
            // FIX: week-year identity for recent-week slots across Dec/Jan
            add(cal.get(Calendar.WEEK_OF_YEAR) to cal.truckingWeekYear())
            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }
    }
}

/** Weeks W1..throughWeek for the given calendar year. */
fun enumerateYearWeekSlots(year: Int, throughWeek: Int): List<Pair<Int, Int>> =
    (1..throughWeek.coerceAtLeast(1)).map { it to year }

/**
 * Reporting weeks whose last day falls in [startDate]…[endDate] (inclusive).
 * A week is owned by exactly one calendar month/year — no overlap.
 */
fun weeksEndingInRange(
    startDate: String,
    endDate: String,
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): List<Pair<Int, Int>> {
    val startParts = parseIsoDateParts(startDate) ?: return emptyList()
    val endParts = parseIsoDateParts(endDate) ?: return emptyList()
    val cal = truckingWeekCalendar(firstDay)
    cal.clear()
    cal.set(startParts.first, startParts.second - 1, startParts.third)
    val endCal = truckingWeekCalendar(firstDay)
    endCal.clear()
    endCal.set(endParts.first, endParts.second - 1, endParts.third)
    if (cal.after(endCal)) return emptyList()
    val lastDay = firstDay.endCalendarDay
    val daysUntilEnd = (lastDay - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, daysUntilEnd)
    val result = mutableListOf<Pair<Int, Int>>()
    while (!cal.after(endCal)) {
        result.add(cal.get(Calendar.WEEK_OF_YEAR) to cal.truckingWeekYear())
        cal.add(Calendar.DAY_OF_YEAR, 7)
    }
    return result
}

/** Недели, принадлежащие месяцу (последний день недели внутри месяца). */
fun getWeeksInMonth(year: Int, month: Int): List<Pair<Int, Int>> {
    val (start, end) = getMonthRange(month, year)
    return weeksEndingInRange(start, end)
}

/** Возвращает (startDate, endDate) для месяца. */
fun getMonthRange(month: Int, year: Int): Pair<String, String> {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    val start = "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, 1)
    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
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
    val cal = Calendar.getInstance()
    cal.set(year, endM - 1, 1)
    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
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
