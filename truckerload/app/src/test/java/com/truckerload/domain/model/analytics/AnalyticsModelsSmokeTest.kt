package com.truckerload.domain.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsModelsSmokeTest {

    @Test
    fun analyticsSummary_holdsAggregates() {
        val summary = AnalyticsSummary(
            totalLoads = 12,
            totalGross = 28_500.0,
            totalMiles = 9_400.0,
            avgRpm = 3.03,
            avgGrossPerLoad = 2_375.0,
            bestWeek = WeekData(
                weekNumber = 29,
                year = 2026,
                label = "W29",
                gross = 6_200.0,
                miles = 2_100.0,
                loadCount = 3,
            ),
        )

        assertEquals(12, summary.totalLoads)
        assertEquals(28_500.0, summary.totalGross, 0.001)
        assertEquals(AnalyticsPeriod.LAST_12_WEEKS, AnalyticsPeriod.values().first())
    }

    @Test
    fun analyticsPeriod_containsExpectedValues() {
        assertEquals(
            listOf(
                AnalyticsPeriod.LAST_12_WEEKS,
                AnalyticsPeriod.LAST_6_MONTHS,
                AnalyticsPeriod.ALL_TIME,
            ),
            AnalyticsPeriod.entries,
        )
    }

    @Test
    fun routeAndDailyData_construct() {
        val route = RouteData(
            origin = "Atlanta, GA",
            destination = "Denver, CO",
            route = "Atlanta, GA → Denver, CO",
            gross = 2_500.0,
            miles = 850.0,
            loadCount = 1,
            rpm = 2.94,
        )
        val daily = DailyData(
            dayLabel = "Mon",
            dayOfWeek = 1,
            gross = 1_200.0,
            loadCount = 1,
        )

        assertEquals("Atlanta, GA", route.origin)
        assertEquals(1, daily.loadCount)
    }
}
