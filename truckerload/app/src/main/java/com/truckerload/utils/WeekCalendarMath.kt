package com.truckerload.utils

import com.truckerload.domain.week.WeekStartDay
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

/** Calendar configured for one reporting-week start (US locale, week 1 contains Jan 1). */
internal fun weekCalendar(firstDay: WeekStartDay): Calendar =
    Calendar.getInstance(Locale.US).apply {
        firstDayOfWeek = firstDay.calendarDay
        minimalDaysInFirstWeek = 1
    }

/**
 * Pure week-number / range math parameterized by [WeekStartDay].
 * Callers that need the driver's current preference pass [WeekStartRuntime] values
 * via the WeekUtils overloads (default = loads week).
 */
object WeekCalendarMath {

    fun numberAndYearFromTimestamp(millis: Long, firstDay: WeekStartDay): Pair<Int, Int> {
        val cal = weekCalendar(firstDay).apply { timeInMillis = millis }
        return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.getWeekYear())
    }

    fun current(firstDay: WeekStartDay, nowMillis: Long = System.currentTimeMillis()): Pair<Int, Int> =
        numberAndYearFromTimestamp(nowMillis, firstDay)

    fun previous(firstDay: WeekStartDay, nowMillis: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val cal = weekCalendar(firstDay).apply { timeInMillis = nowMillis }
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.getWeekYear())
    }

    fun numberAndYearFromDate(dateStr: String?, firstDay: WeekStartDay): Pair<Int, Int> {
        if (dateStr.isNullOrBlank() || dateStr.length < 10) return current(firstDay)
        return try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return current(firstDay)
            val y = parts[0].toIntOrNull() ?: return current(firstDay)
            val m = parts[1].toIntOrNull()?.minus(1) ?: return current(firstDay)
            val d = parts[2].toIntOrNull() ?: return current(firstDay)
            val cal = weekCalendar(firstDay)
            cal.set(y, m, d)
            Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.getWeekYear())
        } catch (_: Exception) {
            current(firstDay)
        }
    }

    fun range(weekNumber: Int, year: Int, firstDay: WeekStartDay): Triple<String, String, String> {
        val cal = weekCalendar(firstDay)
        cal.clear()
        cal.setWeekDate(year, weekNumber, firstDay.calendarDay)
        val startYear = cal.get(Calendar.YEAR)
        val startMonth = cal.get(Calendar.MONTH) + 1
        val startDay = cal.get(Calendar.DAY_OF_MONTH)
        val startStr = String.format(Locale.US, "%04d-%02d-%02d", startYear, startMonth, startDay)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val endYear = cal.get(Calendar.YEAR)
        val endMonth = cal.get(Calendar.MONTH) + 1
        val endDay = cal.get(Calendar.DAY_OF_MONTH)
        val endStr = String.format(Locale.US, "%04d-%02d-%02d", endYear, endMonth, endDay)
        val shortMonths = DateFormatSymbols(Locale.getDefault()).shortMonths
        val startMonthLabel = shortMonths.getOrNull((startMonth - 1).coerceIn(0, 11)).orEmpty().replace(".", "")
        val endMonthLabel = shortMonths.getOrNull((endMonth - 1).coerceIn(0, 11)).orEmpty().replace(".", "")
        val label = "$startMonthLabel $startDay – $endMonthLabel $endDay, $startYear"
        return Triple(startStr, endStr, label)
    }

    fun millisForWeek(weekNumber: Int, year: Int, firstDay: WeekStartDay): Long {
        val cal = weekCalendar(firstDay)
        cal.clear()
        cal.setWeekDate(year, weekNumber, firstDay.calendarDay)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun shift(weekNumber: Int, year: Int, deltaWeeks: Int, firstDay: WeekStartDay): Pair<Int, Int> {
        val cal = weekCalendar(firstDay)
        cal.clear()
        cal.setWeekDate(year, weekNumber, firstDay.calendarDay)
        cal.add(Calendar.WEEK_OF_YEAR, deltaWeeks)
        return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.getWeekYear())
    }

    /** 1 = first day of the week … 7 = last day. */
    fun daysElapsed(firstDay: WeekStartDay, nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = weekCalendar(firstDay).apply { timeInMillis = nowMillis }
        val today = cal.get(Calendar.DAY_OF_WEEK)
        return (today - firstDay.calendarDay + 7) % 7 + 1
    }

    /** Inclusive remaining days (today … week end). */
    fun daysRemaining(firstDay: WeekStartDay, nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = weekCalendar(firstDay).apply { timeInMillis = nowMillis }
        val today = cal.get(Calendar.DAY_OF_WEEK)
        return (firstDay.endCalendarDay - today + 7) % 7 + 1
    }

    fun daysActiveForWeek(
        weekNumber: Int,
        year: Int,
        firstDay: WeekStartDay,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        val (currentWeek, currentYear) = current(firstDay, nowMillis)
        if (weekNumber != currentWeek || year != currentYear) return 7
        return daysElapsed(firstDay, nowMillis).coerceAtLeast(1)
    }

    fun daysRemainingForWeek(
        weekNumber: Int,
        year: Int,
        firstDay: WeekStartDay,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        val (currentWeek, currentYear) = current(firstDay, nowMillis)
        if (weekNumber != currentWeek || year != currentYear) return 1
        return daysRemaining(firstDay, nowMillis).coerceAtLeast(1)
    }
}
