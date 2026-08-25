package com.truckerload.utils

import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.domain.model.analytics.AnalyticsSummary
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsExporterTest {

    @Test
    fun buildCsvContent_emptyDashboard_noCrash() {
        val dashboard = AnalyticsDashboard(
            weeks = emptyList(),
            routes = emptyList(),
            daily = emptyList(),
            summary = AnalyticsSummary(
                totalLoads = 0,
                totalGross = 0.0,
                totalMiles = 0.0,
                avgRpm = 0.0,
                avgGrossPerLoad = 0.0,
                bestWeek = null,
            ),
        )
        val csv = AnalyticsExporter.buildCsvContent(dashboard, AnalyticsPeriod.LAST_12_WEEKS)
        assertTrue(csv.contains("Total loads,0"))
        assertTrue(csv.contains("Paycheck,0.0") || csv.contains("Paycheck,0"))
        assertTrue(csv.contains("Diesel,"))
        assertTrue(!csv.contains("Net profit"))
        assertTrue(csv.contains("Weekly revenue"))
        assertTrue(csv.contains("Top routes"))
        assertTrue(csv.contains("Daily distribution"))
    }
}
