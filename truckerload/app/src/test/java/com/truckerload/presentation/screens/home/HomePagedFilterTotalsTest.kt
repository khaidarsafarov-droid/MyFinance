package com.truckerload.presentation.screens.home

import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePagedFilterTotalsTest {

    @Test
    fun `paged totals calculate weighted average rpm`() {
        val totals = with(HomePagedFilterTotals) {
            WeeklyLoadStatsAgg(
                loadCount = 3,
                totalMiles = 2_290.0,
                totalRevenue = 7_110.0,
            ).toTotals()
        }

        assertEquals(7_110.0 / 2_290.0, totals.avgRpm, 0.0001)
    }

    @Test
    fun `paged totals use zero rpm when miles are zero`() {
        val totals = with(HomePagedFilterTotals) {
            WeeklyLoadStatsAgg(
                loadCount = 0,
                totalMiles = 0.0,
                totalRevenue = 0.0,
            ).toTotals()
        }

        assertEquals(0.0, totals.avgRpm, 0.0)
    }
}
