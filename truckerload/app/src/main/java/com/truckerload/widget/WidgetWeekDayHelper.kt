package com.truckerload.widget

import com.truckerload.domain.week.WeekStartDay
import com.truckerload.domain.week.WeekStartRuntime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Reporting-week chips for the home-screen widget.
 *
 * Fill = load presence. Stroke = "today". Those two signals are independent.
 * Chip order follows the loads week-start day from Settings.
 */
object WidgetWeekDayHelper {

    private val sundayFirstLabels: List<String> = listOf("S", "M", "T", "W", "T", "F", "S")

    val dayLabels: List<String>
        get() = labelsFor(WeekStartRuntime.loads)

    fun labelsFor(firstDay: WeekStartDay): List<String> {
        val startIndex = firstDay.calendarDay - 1
        return (0..6).map { sundayFirstLabels[(startIndex + it) % 7] }
    }

    data class DayChip(
        val label: String,
        val date: LocalDate,
        val hasLoad: Boolean,
        val isToday: Boolean,
        val isFuture: Boolean,
    ) {
        val isPast: Boolean get() = !isToday && !isFuture
    }

    fun startOfWeek(
        today: LocalDate = LocalDate.now(),
        firstDay: DayOfWeek = WeekStartRuntime.loads.javaDayOfWeek,
    ): LocalDate = today.with(TemporalAdjusters.previousOrSame(firstDay))

    /** @deprecated Use [startOfWeek]; kept for existing call sites / tests. */
    fun sundayOfWeek(today: LocalDate = LocalDate.now()): LocalDate = startOfWeek(today)

    fun maskFromIsoDates(isoDates: Collection<String>, weekStart: LocalDate): Int {
        var mask = 0
        isoDates.forEach { raw ->
            val iso = raw.take(10)
            if (iso.length < 10) return@forEach
            val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return@forEach
            val offset = ChronoUnit.DAYS.between(weekStart, date).toInt()
            if (offset in 0..6) mask = mask or (1 shl offset)
        }
        return mask
    }

    fun chips(
        weekLoadMask: Int,
        today: LocalDate = LocalDate.now(),
        weekStart: LocalDate = startOfWeek(today),
    ): List<DayChip> {
        val labels = labelsFor(WeekStartDay.fromJavaDayOfWeek(weekStart.dayOfWeek))
        return (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            DayChip(
                label = labels[offset],
                date = date,
                hasLoad = (weekLoadMask shr offset) and 1 == 1,
                isToday = date == today,
                isFuture = date.isAfter(today),
            )
        }
    }
}
