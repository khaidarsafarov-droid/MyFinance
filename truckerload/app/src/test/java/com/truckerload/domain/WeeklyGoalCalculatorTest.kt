package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyGoalCalculatorTest {

    @Test
    fun `display percent is real above 100 and safe at goal zero`() {
        assertEquals(0f, WeeklyGoalCalculator.displayPercent(0L, 100_000L), 0.01f)
        assertEquals(50f, WeeklyGoalCalculator.displayPercent(50_000L, 100_000L), 0.01f)
        assertEquals(100f, WeeklyGoalCalculator.displayPercent(100_000L, 100_000L), 0.01f)
        assertEquals(130f, WeeklyGoalCalculator.displayPercent(130_000L, 100_000L), 0.01f)
        assertEquals(245f, WeeklyGoalCalculator.displayPercent(245_000L, 100_000L), 0.01f)
        assertEquals(0f, WeeklyGoalCalculator.displayPercent(130_000L, 0L), 0.01f)
        assertEquals(0f, WeeklyGoalCalculator.displayPercent(130_000L, -5L), 0.01f)
        assertEquals(0f, WeeklyGoalCalculator.displayPercent(-100L, 100_000L), 0.01f)
    }

    @Test
    fun `calculate keeps the real percent when gross exceeds the goal`() {
        val progress = WeeklyGoalCalculator.calculate(
            targetAmount = Money.fromDollars(1_000.0),
            weekLoads = emptyList(),
            weekNumber = 1,
            year = 2020,
            sqlYield = WeekYieldSnapshot(totalGross = Money.fromDollars(1_300.0), totalActiveDays = 0.0),
        )
        assertEquals(130f, progress.progressPercent, 0.01f)
        assertEquals(Money.fromDollars(1_300.0), progress.currentGross)
        assertEquals(Money.ZERO, progress.remainingAmount)
        assertEquals(PaceStatus.GOAL_MET, progress.paceStatus)

        val emptyGoal = WeeklyGoalCalculator.calculate(
            targetAmount = Money.ZERO,
            weekLoads = emptyList(),
            weekNumber = 1,
            year = 2020,
            sqlYield = WeekYieldSnapshot(totalGross = Money.fromDollars(1_300.0), totalActiveDays = 0.0),
        )
        assertEquals(0f, emptyGoal.progressPercent, 0.01f)
    }

    @Test
    fun `daily pace uses active days from loads`() {
        val loads = listOf(
            Load(
                id = "1",
                tripId = "T1",
                date = "2026-06-10",
                totalRate = Money.fromDollars(2000.0),
                totalMiles = 800.0,
                pointA = "Austin, TX",
                pointB = "Dallas, TX",
                puCount = 1,
                delCount = 1,
                weekNumber = 24,
                year = 2026,
                rawMessage = "",
                parsedAt = 1L,
                updatedAt = 1L,
                durationDays = 2.0,
            ),
        )
        val pace = WeeklyGoalCalculator.calculateDailyPace(loads)
        assertEquals(Money.fromDollars(1000.0), pace)
    }

    @Test
    fun `progress marks behind when yield is low`() {
        val loads = listOf(
            Load(
                id = "1",
                tripId = "T1",
                date = "2026-06-10",
                totalRate = Money.fromDollars(500.0),
                totalMiles = 400.0,
                pointA = "Austin, TX",
                pointB = "Dallas, TX",
                puCount = 1,
                delCount = 1,
                weekNumber = 24,
                year = 2026,
                rawMessage = "",
                parsedAt = 1L,
                updatedAt = 1L,
                durationDays = 1.0,
            ),
        )
        val progress = WeeklyGoalCalculator.calculate(
            targetAmount = Money.fromDollars(5000.0),
            weekLoads = loads,
            weekNumber = 24,
            year = 2026,
        )
        assertEquals(PaceStatus.BEHIND, progress.paceStatus)
        assertEquals(Money.fromDollars(500.0), progress.currentGross)
    }

    @Test
    fun `gross prefers in-memory loads when SQL snapshot undercounts`() {
        val loads = listOf(
            Load(
                id = "1",
                tripId = "T1",
                date = "2026-06-10",
                totalRate = Money.fromDollars(2500.0),
                totalMiles = 800.0,
                pointA = "Austin, TX",
                pointB = "Dallas, TX",
                puCount = 1,
                delCount = 1,
                weekNumber = 0,
                year = 0,
                rawMessage = "",
                parsedAt = 1L,
                updatedAt = 1L,
                durationDays = 1.0,
            ),
        )
        val progress = WeeklyGoalCalculator.calculate(
            targetAmount = Money.fromDollars(5000.0),
            weekLoads = loads,
            weekNumber = 24,
            year = 2026,
            sqlYield = WeekYieldSnapshot(totalGross = Money.fromDollars(0.0), totalActiveDays = 0.0),
        )
        assertEquals(Money.fromDollars(2500.0), progress.currentGross)
    }

    @Test
    fun `yield uses in-memory loads when SQL snapshot undercounts`() {
        val loads = listOf(
            Load(
                id = "1",
                tripId = "T1",
                date = "2026-06-10",
                totalRate = Money.fromDollars(2500.0),
                totalMiles = 800.0,
                pointA = "Austin, TX",
                pointB = "Dallas, TX",
                puCount = 1,
                delCount = 1,
                weekNumber = 0,
                year = 0,
                rawMessage = "",
                parsedAt = 1L,
                updatedAt = 1L,
                durationDays = 1.0,
            ),
        )
        val progress = WeeklyGoalCalculator.calculate(
            targetAmount = Money.fromDollars(5000.0),
            weekLoads = loads,
            weekNumber = 24,
            year = 2026,
            sqlYield = WeekYieldSnapshot(
                totalGross = Money.fromDollars(100.0),
                totalActiveDays = 5.0,
            ),
        )
        assertEquals(Money.fromDollars(2500.0), progress.currentGross)
        assertEquals(Money.fromDollars(2500.0), progress.actualDailyYield)
        assertEquals(1.0, progress.totalActiveDays, 0.01)
    }
}
