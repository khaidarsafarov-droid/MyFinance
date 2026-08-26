package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import com.truckerload.domain.week.WeekStartDay
import com.truckerload.domain.week.WeekStartRuntime
import java.util.Calendar
import java.util.Locale

/** Reporting-week calendar. Default is the loads week start from Settings. */
internal fun truckingWeekCalendar(firstDay: WeekStartDay = WeekStartRuntime.loads): Calendar =
    weekCalendar(firstDay)

private fun weekSortKey(weekNumber: Int, year: Int): Long = year * 100L + weekNumber

private fun isWeekAfter(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean =
    weekSortKey(a.first, a.second) > weekSortKey(b.first, b.second)

/**
 * Week-year for Sun–Sat trucking weeks (API 24+ [Calendar.getWeekYear]).
 * Late December dates that belong to week 1 of the next calendar year must use
 * that week-year (e.g. 2025-12-28 → week 1 of 2026), not [Calendar.YEAR].
 */
internal fun Calendar.truckingWeekYear(): Int = getWeekYear()

/** Get (weekNumber, weekYear) from timestamp (millis) in device default timezone. */
fun getWeekNumberAndYearFromTimestamp(
    millis: Long,
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Pair<Int, Int> = WeekCalendarMath.numberAndYearFromTimestamp(millis, firstDay)

/**
 * Returns (weekStartDate "yyyy-MM-dd", weekEndDate "yyyy-MM-dd", weekLabel) for the given week.
 */
fun getWeekRange(
    weekNumber: Int,
    year: Int,
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Triple<String, String, String> = WeekCalendarMath.range(weekNumber, year, firstDay)

fun getCurrentWeekNumberAndYear(
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Pair<Int, Int> = WeekCalendarMath.current(firstDay)

/** Noon (local) on the first day of the reporting week — keeps week chips aligned with save(). */
fun getMillisForWeek(
    weekNumber: Int,
    year: Int,
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Long = WeekCalendarMath.millisForWeek(weekNumber, year, firstDay)

fun shiftWeekNumberAndYear(
    weekNumber: Int,
    year: Int,
    deltaWeeks: Int,
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Pair<Int, Int> = WeekCalendarMath.shift(weekNumber, year, deltaWeeks, firstDay)

fun getPreviousWeekNumberAndYear(
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Pair<Int, Int> = WeekCalendarMath.previous(firstDay)

/** Current calendar week start and end as "YYYY-MM-DD" (device local). Use for filtering loads by date. */
fun getCurrentWeekStartAndEnd(
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Pair<String, String> {
    val (week, year) = getCurrentWeekNumberAndYear(firstDay)
    val (start, end, _) = getWeekRange(week, year, firstDay)
    return Pair(start, end)
}

/** Days elapsed in current reporting week (first day = 1 … today). */
fun getDaysElapsedInCurrentWeek(
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Int = WeekCalendarMath.daysElapsed(firstDay)

/** Days remaining in current reporting week including today. */
fun getDaysRemainingInCurrentWeek(
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Int = WeekCalendarMath.daysRemaining(firstDay)

/** Alias for widget / UI copy. */
fun getDaysLeftInWeek(firstDay: WeekStartDay = WeekStartRuntime.loads): Int =
    getDaysRemainingInCurrentWeek(firstDay)

/** Total days in a reporting week (always 7). */
fun getDaysInTruckingWeek(): Int = 7

/** Days active in week (start…today inclusive), min 1. Past weeks → 7. */
fun getDaysActiveForWeek(
    weekNumber: Int,
    year: Int,
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Int = WeekCalendarMath.daysActiveForWeek(weekNumber, year, firstDay)

/** Days remaining in week (today…end inclusive), min 1 for math. Past weeks → 1. */
fun getDaysRemainingForWeek(
    weekNumber: Int,
    year: Int,
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Int = WeekCalendarMath.daysRemainingForWeek(weekNumber, year, firstDay)

/** Вчера в формате YYYY-MM-DD. */
fun getYesterdayDate(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    return "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

/** Прошлая неделя: (startDate, endDate) в формате YYYY-MM-DD. */
fun getLastWeekStartAndEnd(
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Pair<String, String> {
    val (week, year) = getPreviousWeekNumberAndYear(firstDay)
    val (start, end, _) = getWeekRange(week, year, firstDay)
    return Pair(start, end)
}

/** Дата первого Pick Up (PU) или load.date. */
fun getPickUpDate(load: Load): String? {
    val yearHint = load.date.take(4).toIntOrNull()
    val fromStops = load.stops
        .filter { it.type == StopType.PU }
        .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, yearHint, trustDefaultYear = yearHint != null) }
        .minOrNull()
    return fromStops ?: load.date.takeIf { it.length >= 10 }
}

/** Дата последней доставки (DEL), если есть. */
fun getDeliveryDate(load: Load): String? {
    val yearHint = load.date.take(4).toIntOrNull()
    return load.stops
        .filter { it.type == StopType.DEL }
        .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, yearHint, trustDefaultYear = yearHint != null) }
        .maxOrNull()
}

/**
 * Неделя отчёта для груза: по дате PU (начало недели из настроек грузов).
 * Если доставка попадает в более позднюю неделю (напр. PU суббота, DEL воскресенье) — неделя доставки.
 */
fun getLoadReportingWeek(load: Load): Pair<Int, Int> {
    val start = WeekStartRuntime.loads
    val puDate = getPickUpDate(load)
    val delDate = getDeliveryDate(load)
    val puWeek = getWeekNumberAndYearFromDate(puDate, start)
    if (delDate == null) return puWeek
    val delWeek = getWeekNumberAndYearFromDate(delDate, start)
    return if (isWeekAfter(delWeek, puWeek)) delWeek else puWeek
}

fun isLoadInWeek(load: Load, weekNumber: Int, year: Int): Boolean {
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

/** Year hint from load.date (YYYY-MM-DD…) for Relay MM/DD stop times. */
private fun loadDateYearHint(load: Load): Int? =
    load.date.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()

/** First PU stop datetime (millis) or load.date at start of day. */
fun getFirstPickUpMillis(load: Load): Long? {
    val yearHint = loadDateYearHint(load)
    val fromStops = load.stops
        .filter { it.type == StopType.PU }
        .mapNotNull { parseScheduledTimeToMillis(it.scheduledTime, yearHint, trustDefaultYear = yearHint != null) }
        .minOrNull()
    if (fromStops != null) return fromStops
    return load.date.takeIf { it.length >= 10 }?.let { dateStringToStartOfDayMillis(it) }
}

/** Last DEL stop datetime (millis) or load.date at start of day. */
fun getLastDeliveryMillis(load: Load): Long? {
    val yearHint = loadDateYearHint(load)
    val puMs = getFirstPickUpMillis(load)
    val fromStops = load.stops
        .filter { it.type == StopType.DEL }
        .mapNotNull { stop ->
            val ms = parseScheduledTimeToMillis(stop.scheduledTime, yearHint, trustDefaultYear = yearHint != null)
                ?: return@mapNotNull null
            // FIX: New Year trip (12/30 → 01/02) — bump DEL into next year when before PU
            if (puMs != null && ms < puMs) {
                Calendar.getInstance(Locale.getDefault()).apply {
                    timeInMillis = ms
                    add(Calendar.YEAR, 1)
                }.timeInMillis
            } else {
                ms
            }
        }
        .maxOrNull()
    if (fromStops != null) return fromStops
    return load.date.takeIf { it.length >= 10 }?.let { dateStringToStartOfDayMillis(it) }
}

/** Для груза возвращает множество дат (YYYY-MM-DD), когда груз активен: от первой до последней остановки включительно. */
fun getLoadDateRange(load: Load): Set<String> {
    val dates = mutableSetOf<String>()
    canonicalDateString(load.date)?.let { dates.add(it) }
    val yearHint = load.date.take(4).toIntOrNull()
    val stopDates = load.stops.mapNotNull {
        parseDateFromScheduledTime(it.scheduledTime, yearHint, trustDefaultYear = yearHint != null)
    }
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

/**
 * Parse "YYYY-MM-DD" to (weekNumber, weekYear). On error returns current week/year.
 */
fun getWeekNumberAndYearFromDate(
    dateStr: String?,
    firstDay: WeekStartDay = WeekStartRuntime.loads,
): Pair<Int, Int> = WeekCalendarMath.numberAndYearFromDate(dateStr, firstDay)
