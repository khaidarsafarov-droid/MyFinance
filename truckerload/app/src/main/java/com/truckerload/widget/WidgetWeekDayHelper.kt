package com.truckerload.widget

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Sun–Sat trucking week chips for the home-screen widget. */
object WidgetWeekDayHelper {

    enum class DayStatus { PAST, TODAY, FUTURE }

    val dayLabels: List<String> = listOf("S", "M", "T", "W", "T", "F", "S")

    fun statusesForCurrentCalendarWeek(): List<DayStatus> {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            when {
                date.isBefore(today) -> DayStatus.PAST
                date.isEqual(today) -> DayStatus.TODAY
                else -> DayStatus.FUTURE
            }
        }
    }
}
