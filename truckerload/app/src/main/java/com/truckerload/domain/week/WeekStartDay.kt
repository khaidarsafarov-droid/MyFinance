package com.truckerload.domain.week

import java.time.DayOfWeek
import java.util.Calendar

/**
 * First day of a reporting week. Independently configurable for loads vs diesel.
 * Default is Sunday (US trucking / Amazon Relay settlement).
 */
enum class WeekStartDay(val calendarDay: Int) {
    SUNDAY(Calendar.SUNDAY),
    MONDAY(Calendar.MONDAY),
    TUESDAY(Calendar.TUESDAY),
    WEDNESDAY(Calendar.WEDNESDAY),
    THURSDAY(Calendar.THURSDAY),
    FRIDAY(Calendar.FRIDAY),
    SATURDAY(Calendar.SATURDAY),
    ;

    val javaDayOfWeek: DayOfWeek
        get() = when (this) {
            SUNDAY -> DayOfWeek.SUNDAY
            MONDAY -> DayOfWeek.MONDAY
            TUESDAY -> DayOfWeek.TUESDAY
            WEDNESDAY -> DayOfWeek.WEDNESDAY
            THURSDAY -> DayOfWeek.THURSDAY
            FRIDAY -> DayOfWeek.FRIDAY
            SATURDAY -> DayOfWeek.SATURDAY
        }

    /** Calendar.DAY_OF_WEEK of the last day in this week (6 days after [calendarDay]). */
    val endCalendarDay: Int
        get() = ((calendarDay - 1 + 6) % 7) + 1

    companion object {
        val DEFAULT: WeekStartDay = SUNDAY

        fun fromCalendarDay(day: Int): WeekStartDay =
            entries.firstOrNull { it.calendarDay == day } ?: DEFAULT

        fun fromJavaDayOfWeek(day: DayOfWeek): WeekStartDay =
            entries.firstOrNull { it.javaDayOfWeek == day } ?: DEFAULT
    }
}
