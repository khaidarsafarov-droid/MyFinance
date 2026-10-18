package com.truckerload.domain.goal

import com.truckerload.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeklyDayPlanTest {

    private val needed = Money.fromCents(171_559L)

    @Test
    fun sundayWeek_todayIsFirst_restAreFutureNeeded() {
        val sunday = LocalDate.of(2026, 10, 18)
        val rows = WeeklyDayPlan.rows(
            weekStart = sunday,
            today = sunday,
            grossCentsBySqliteDow = mapOf(0 to 99_090L),
            dailyNeeded = needed,
        )

        assertEquals(7, rows.size)
        assertEquals(sunday, rows.first().date)
        assertEquals(LocalDate.of(2026, 10, 24), rows.last().date)
        assertEquals(DayPlanStatus.TODAY, rows.first().status)
        assertEquals(58, rows.first().planPercent)
        assertEquals(Money.fromCents(99_090L), rows.first().gross)
        rows.drop(1).forEach { row ->
            assertEquals(DayPlanStatus.FUTURE, row.status)
            assertEquals(Money.ZERO, row.gross)
        }
    }

    @Test
    fun mondayWeek_marksPastMetMissedTodayAndFuture() {
        val monday = LocalDate.of(2026, 10, 19)
        val wednesday = LocalDate.of(2026, 10, 21)
        val rows = WeeklyDayPlan.rows(
            weekStart = monday,
            today = wednesday,
            grossCentsBySqliteDow = mapOf(
                1 to 200_000L,
                2 to 10_000L,
                3 to 50_000L,
            ),
            dailyNeeded = needed,
        )

        assertEquals(listOf(monday.plusDays(0), monday.plusDays(6)), listOf(rows.first().date, rows.last().date))
        assertEquals(DayPlanStatus.PAST_MET, rows[0].status)
        assertEquals(DayPlanStatus.PAST_MISSED, rows[1].status)
        assertEquals(DayPlanStatus.TODAY, rows[2].status)
        assertEquals(50_000L, rows[2].gross.cents)
        rows.drop(3).forEach { assertEquals(DayPlanStatus.FUTURE, it.status) }
    }

    @Test
    fun percentRoundsHalfUpAndZeroNeedStaysZero() {
        assertEquals(58, WeeklyDayPlan.percentOf(Money.fromCents(99_090L), needed))
        assertEquals(0, WeeklyDayPlan.percentOf(Money.ZERO, Money.ZERO))
        assertEquals(100, WeeklyDayPlan.percentOf(Money.fromCents(1L), Money.ZERO))
    }
}
