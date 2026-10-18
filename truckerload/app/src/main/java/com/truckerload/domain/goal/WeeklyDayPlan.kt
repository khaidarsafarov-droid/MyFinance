package com.truckerload.domain.goal

import com.truckerload.domain.money.Money
import java.time.LocalDate

enum class DayPlanStatus {
    PAST_MET,
    PAST_MISSED,
    TODAY,
    FUTURE,
}

data class DayPlanRow(
    val date: LocalDate,
    /** `strftime('%w')`: Sunday is 0. */
    val sqliteDow: Int,
    val gross: Money,
    val status: DayPlanStatus,
    /** Rounded actual ÷ daily need, in percent. Zero when no daily need is set. */
    val planPercent: Int,
    /** 0..1 for the today ring. */
    val planFraction: Float,
)

/**
 * Seven rows for the reporting week, in week-start order.
 * Gross is keyed by SQLite weekday so it matches [com.truckerload.data.local.dao.LoadDao.getDailyDistribution].
 */
object WeeklyDayPlan {
    fun rows(
        weekStart: LocalDate,
        today: LocalDate,
        grossCentsBySqliteDow: Map<Int, Long>,
        dailyNeeded: Money,
    ): List<DayPlanRow> = (0..6).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        val dow = date.dayOfWeek.value % 7
        val gross = Money.fromCents(grossCentsBySqliteDow[dow] ?: 0L)
        val status = when {
            date.isBefore(today) -> if (met(gross, dailyNeeded)) DayPlanStatus.PAST_MET else DayPlanStatus.PAST_MISSED
            date.isAfter(today) -> DayPlanStatus.FUTURE
            else -> DayPlanStatus.TODAY
        }
        DayPlanRow(
            date = date,
            sqliteDow = dow,
            gross = gross,
            status = status,
            planPercent = percentOf(gross, dailyNeeded),
            planFraction = fractionOf(gross, dailyNeeded),
        )
    }

    fun percentOf(gross: Money, needed: Money): Int {
        if (needed.cents <= 0L) return if (gross.cents > 0L) 100 else 0
        val rounded = (gross.cents * 100L + needed.cents / 2L) / needed.cents
        return rounded.toInt().coerceAtLeast(0)
    }

    private fun met(gross: Money, needed: Money): Boolean =
        if (needed.cents <= 0L) gross.cents > 0L else gross.cents >= needed.cents

    private fun fractionOf(gross: Money, needed: Money): Float = when {
        needed.cents <= 0L -> if (gross.cents > 0L) 1f else 0f
        else -> (gross.cents.toFloat() / needed.cents.toFloat()).coerceIn(0f, 1f)
    }
}
