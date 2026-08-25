package com.truckerload.utils

import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.domain.model.analytics.AnalyticsSummary
import com.truckerload.domain.model.analytics.DailyData
import com.truckerload.domain.model.analytics.PeriodFinance
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.domain.model.analytics.WeekData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsReportBuilderTest {

    @Test
    fun buildCsvContent_emptyDashboard_noCrash() {
        val csv = AnalyticsReportBuilder.buildCsvContent(emptyDashboard(), englishLabels())
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("Loads,0"))
        assertTrue(!csv.contains("Driver:"))
        assertTrue(csv.contains("Paycheck,0.00"))
        assertTrue(csv.contains("Diesel,"))
        assertFalse(csv.contains("Net profit"))
        assertTrue(csv.contains("Weekly revenue"))
        assertTrue(csv.contains("Top routes"))
        assertTrue(csv.contains("Daily distribution"))
        assertTrue(csv.contains("Period: Last 12 weeks"))
    }

    @Test
    fun buildReadableText_formatsMoneyAndListsRows() {
        val week = WeekData(
            weekNumber = 12,
            year = 2026,
            label = "W12 2026",
            gross = 18862.0,
            miles = 6200.0,
            loadCount = 8,
        )
        val dashboard = AnalyticsDashboard(
            weeks = listOf(week),
            routes = listOf(
                RouteData(
                    origin = "Garner, NC",
                    destination = "Dallas, TX",
                    route = "Garner, NC → Dallas, TX",
                    gross = 2500.0,
                    miles = 850.0,
                    loadCount = 1,
                    rpm = 2.94,
                ),
            ),
            daily = listOf(
                DailyData(dayLabel = "Mon", dayOfWeek = 1, gross = 4800.0, loadCount = 2),
            ),
            summary = AnalyticsSummary(
                totalLoads = 47,
                totalGross = 112619.0,
                totalMiles = 38214.0,
                avgRpm = 2.95,
                avgGrossPerLoad = 2396.0,
                bestWeek = week,
            ),
            finance = PeriodFinance(
                paycheckTotal = 90000.0,
                dieselTotal = 12000.0,
                dieselGallons = 3000.0,
                dieselSavings = 450.0,
            ),
        )
        val text = AnalyticsReportBuilder.buildReadableText(dashboard, englishLabels(ownerName = "Ivan Petrov"))
        assertTrue(text.contains("TruckoRig — My numbers"))
        assertTrue(text.contains("Period: Last 12 weeks"))
        assertTrue(text.contains("Driver: Ivan Petrov"))
        assertTrue(text.contains("• Loads: 47"))
        assertTrue(text.contains("$112,619"))
        assertTrue(text.contains("$2.95/mi"))
        assertTrue(text.contains("Paycheck: $90,000"))
        assertTrue(text.contains("Diesel: $12,000"))
        assertTrue(text.contains("Diesel discount saved: $450"))
        assertTrue(text.contains("W12 2026: $18,862"))
        assertTrue(text.contains("Garner, NC → Dallas, TX"))
        assertTrue(text.contains("• Mon: $4,800"))
        assertFalse(text.contains("Net profit"))
        assertFalse(text.contains("осталось"))
    }

    private fun emptyDashboard() = AnalyticsDashboard(
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

    private fun englishLabels(ownerName: String = "") = AnalyticsExportLabels(
        appName = "TruckoRig",
        title = "My numbers",
        period = "Last 12 weeks",
        periodLine = "Period: Last 12 weeks",
        summarySection = "Summary",
        totalLoads = "Loads",
        totalGross = "Earned from loads",
        totalMiles = "Mileage",
        avgRpm = "Avg per mile",
        avgPerLoad = "Avg per load",
        bestWeek = "Best week",
        financeSection = "Money for this period",
        paycheck = "Paycheck",
        diesel = "Diesel",
        gallons = "Gallons",
        avgPrice = "Avg price per gallon",
        savings = "Diesel discount saved",
        weeklySection = "Weekly revenue",
        weekColumn = "Week",
        yearColumn = "Year",
        amountColumn = "Amount",
        milesColumn = "Mileage",
        loadsColumn = "Loads",
        routesSection = "Top routes",
        routeColumn = "Route",
        rpmColumn = "Avg per mile",
        dailySection = "Daily distribution",
        dayColumn = "Day",
        empty = "No data for this period yet",
        ownerLabel = "Driver",
        ownerName = ownerName,
    )
}
