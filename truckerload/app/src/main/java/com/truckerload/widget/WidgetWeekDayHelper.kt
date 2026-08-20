package com.truckerload.widget

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Sun–Sat trucking-week chips for the home-screen widget.
 *
 * Fill = load presence. Stroke = "today". Those two signals are independent.
 */
object WidgetWeekDayHelper {

    val dayLabels: List<String> = listOf("S", "M", "T", "W", "T", "F", "S")

    data class DayChip(
        val label: String,
        val date: LocalDate,
        val hasLoad: Boolean,
        val isToday: Boolean,
        val isFuture: Boolean,
    ) {
        val isPast: Boolean get() = !isToday && !isFuture
    }

    fun sundayOfWeek(today: LocalDate = LocalDate.now()): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

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
        weekStart: LocalDate = sundayOfWeek(today),
    ): List<DayChip> = (0..6).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        DayChip(
            label = dayLabels[offset],
            date = date,
            hasLoad = (weekLoadMask shr offset) and 1 == 1,
            isToday = date == today,
            isFuture = date.isAfter(today),
        )
    }
}
