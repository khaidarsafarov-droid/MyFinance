package com.truckerload.presentation.screens.home

import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.presentation.screens.home.HomePagedFilterTotals.toTotals
import org.junit.Assert.assertEquals
import org.junit.Test
import com.truckerload.domain.money.Money

/**
 * SQL-paged week headers must carry the same avg RPM as the in-memory
 * [com.truckerload.domain.filter.LoadFilterUseCase.calculateTotals] path,
 * otherwise the Home period card shows $0.00 / mi for a non-empty week.
 */
class HomePagedFilterTotalsTest {

    @Test
    fun toTotals_derivesAvgRpmFromSqlAggregate() {
        val totals = WeeklyLoadStatsAgg(
            loadCount = 4,
            totalMiles = 1807.0,
            totalRevenue = Money.fromDollars(4630.0).cents,
        ).toTotals()

        assertEquals(4, totals.loadCount)
        assertEquals(Money.fromDollars(4630.0), totals.totalRate)
        assertEquals(1807.0, totals.totalMiles, 0.001)
        assertEquals(4630.0 / 1807.0, totals.avgRpm, 0.0001)
    }

    @Test
    fun toTotals_zeroMilesKeepsRpmAtZero() {
        val totals = WeeklyLoadStatsAgg(
            loadCount = 0,
            totalMiles = 0.0,
            totalRevenue = Money.fromDollars(0.0).cents,
        ).toTotals()

        assertEquals(0.0, totals.avgRpm, 0.0001)
    }
}
