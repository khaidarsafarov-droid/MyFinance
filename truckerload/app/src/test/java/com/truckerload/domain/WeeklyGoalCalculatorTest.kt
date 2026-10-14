package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Test
import com.truckerload.domain.money.Money

class WeeklyGoalCalculatorTest {

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
