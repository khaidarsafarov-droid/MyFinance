package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.sum
import com.truckerload.utils.getLoadReportingWeek
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.isLoadInWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Closed-week gross stays on the in-memory reporting week. A persisted
 * weekNumber/year sum (the shape of watchWeekYieldAgg) is not the same number.
 */
class ClosedWeekGrossTest {

    @Test
    fun sharedListMatchesLegacyFilterWhenPersistedWeekIsStale() {
        val stale = load(
            id = "stale",
            date = "2026-06-10",
            dollars = 2500.0,
            weekNumber = 0,
            year = 0,
        )
        val (week, year) = getLoadReportingWeek(stale)
        val loads = listOf(stale)

        val legacy = legacyClosedWeekGross(loads, week, year)
        assertEquals(legacy, ClosedWeekGross.dollars(loads, week, year), 0.0)
        assertEquals(2500.0, legacy, 0.0)

        val persistedWeek = persistedWeekGross(loads, week, year)
        assertNotEquals(legacy, persistedWeek)
        assertEquals(0.0, persistedWeek, 0.0)
    }

    @Test
    fun sharedListMatchesLegacyFilterWhenFinishWeekDiffersFromPersistedWeek() {
        val pickup = "2026-06-06"
        val finish = "2026-06-07"
        val pickupWeek = getWeekNumberAndYearFromDate(pickup)
        val finishWeek = getWeekNumberAndYearFromDate(finish)
        assertNotEquals(pickupWeek, finishWeek)

        val shifted = load(
            id = "shifted",
            date = pickup,
            dollars = 800.0,
            weekNumber = pickupWeek.first,
            year = pickupWeek.second,
        ).copy(actualFinishDate = finish)
        val (week, year) = getLoadReportingWeek(shifted)
        assertEquals(finishWeek, week to year)

        val loads = listOf(shifted)
        val legacy = legacyClosedWeekGross(loads, week, year)
        assertEquals(legacy, ClosedWeekGross.dollars(loads, week, year), 0.0)
        assertEquals(800.0, legacy, 0.0)
        assertEquals(0.0, persistedWeekGross(loads, week, year), 0.0)
    }

    @Test
    fun sharedListMatchesPersistedWeekWhenTheStoredWeekIsAlreadyTheReportingWeek() {
        val aligned = load(
            id = "aligned",
            date = "2026-06-10",
            dollars = 1200.0,
            weekNumber = 0,
            year = 0,
        ).let { row ->
            val (week, year) = getLoadReportingWeek(row)
            row.copy(weekNumber = week, year = year)
        }
        val (week, year) = getLoadReportingWeek(aligned)
        val loads = listOf(aligned)
        val legacy = legacyClosedWeekGross(loads, week, year)
        assertEquals(legacy, ClosedWeekGross.dollars(loads, week, year), 0.0)
        assertEquals(legacy, persistedWeekGross(loads, week, year), 0.0)
        assertTrue(legacy > 0.0)
    }

    @Test
    fun grossPaceAndDaysAreReadFromTheSameProgress() {
        val row = load(
            id = "current",
            date = "2026-06-10",
            dollars = 2500.0,
            weekNumber = 0,
            year = 0,
        )
        val (closedWeek, closedYear) = getLoadReportingWeek(row)
        val loads = listOf(row)
        val progress = WeeklyGoalCalculator.calculateCurrentWeek(
            targetAmount = Money.fromDollars(5000.0),
            allLoads = loads,
            sqlYield = WeekYieldSnapshot(totalGross = Money.ZERO, totalActiveDays = 0.0),
        )
        val gross = progress.currentGross.cents / 100.0
        val pace = progress.dailyTargetNeeded.cents / 100.0
        val days = progress.daysRemainingInWeek
        val yield = progress.actualDailyYield.cents / 100.0
        assertEquals(progress.currentGross.cents.div(100.0), gross, 0.0)
        assertEquals(progress.dailyTargetNeeded.cents.div(100.0), pace, 0.0)
        assertEquals(progress.daysRemainingInWeek, days)
        assertEquals(progress.actualDailyYield.cents.div(100.0), yield, 0.0)
        assertEquals(legacyClosedWeekGross(loads, closedWeek, closedYear), ClosedWeekGross.dollars(loads, closedWeek, closedYear), 0.0)
    }

    private fun legacyClosedWeekGross(loads: List<Load>, week: Int, year: Int): Double =
        loads.filter { isLoadInWeek(it, week, year) }.map { it.totalRate }.sum().cents / 100.0

    private fun persistedWeekGross(loads: List<Load>, week: Int, year: Int): Double =
        loads.filter { it.weekNumber == week && it.year == year }.map { it.totalRate }.sum().cents / 100.0

    private fun load(
        id: String,
        date: String,
        dollars: Double,
        weekNumber: Int,
        year: Int,
    ) = Load(
        id = id,
        tripId = "T-$id",
        date = date,
        totalRate = Money.fromDollars(dollars),
        totalMiles = 800.0,
        pointA = "Austin, TX",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = weekNumber,
        year = year,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        durationDays = 1.0,
    )
}
