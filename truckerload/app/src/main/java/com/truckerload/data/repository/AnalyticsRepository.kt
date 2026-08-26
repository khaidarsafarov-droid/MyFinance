package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.domain.analytics.RouteDisplayHelper
import com.truckerload.domain.model.DieselPurchaseMath
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.analytics.AnalyticsFilter
import com.truckerload.domain.model.analytics.AnalyticsSummary
import com.truckerload.domain.model.analytics.DailyData
import com.truckerload.domain.model.analytics.PeriodFinance
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.domain.model.analytics.WeekData
import com.truckerload.utils.dateBounds
import com.truckerload.utils.weekSlots

class AnalyticsRepository(private val db: AppDatabase) {

    private val loadDao = db.loadDao()
    private val stopDao = db.stopDao()
    private val paycheckDao = db.paycheckDao()
    private val dieselDao = db.dieselDao()

    suspend fun loadDashboard(filter: AnalyticsFilter): AnalyticsDashboard {
        val bounds = filter.dateBounds()
        val minDate = bounds.minDate
        val maxDate = bounds.maxDate
        val weekRows = loadDao.getWeeklyRevenue(minDate, maxDate)
        val dataByKey = weekRows.associateBy { it.weekNumber to it.year }
        val slots = weekSlotsFor(filter, weekRows)
        val weeks = slots.map { (weekNumber, year) ->
            val row = dataByKey[weekNumber to year]
            WeekData(
                weekNumber = weekNumber,
                year = year,
                label = "W$weekNumber",
                gross = row?.gross ?: 0.0,
                miles = row?.miles ?: 0.0,
                loadCount = row?.loadCount ?: 0,
            )
        }
        val routes = loadTopRoutes(minDate, maxDate, 5)
        val daily = mapDailyDistribution(loadDao.getDailyDistribution(minDate, maxDate))
        val totals = loadDao.getAnalyticsTotals(minDate, maxDate)
        val avgRpm = if (totals.miles > 0) totals.gross / totals.miles else 0.0
        val avgPerLoad = if (totals.loadCount > 0) totals.gross / totals.loadCount else 0.0
        val bestWeek = weeks.maxByOrNull { it.gross }
        val summary = AnalyticsSummary(
            totalLoads = totals.loadCount,
            totalGross = totals.gross,
            totalMiles = totals.miles,
            avgRpm = avgRpm,
            avgGrossPerLoad = avgPerLoad,
            bestWeek = bestWeek,
        )
        return AnalyticsDashboard(
            weeks = weeks,
            routes = routes,
            daily = daily,
            summary = summary,
            finance = loadFinance(minDate, maxDate),
        )
    }

    /**
     * Paycheck follows the week rule used elsewhere in the journal: one settlement
     * per week, so extra rows for the same week are ignored instead of double-counted.
     */
    private suspend fun loadFinance(minDate: String, maxDate: String): PeriodFinance {
        val paycheckTotal = paycheckDao.getPaychecksSince(minDate, maxDate)
            .groupBy { it.weekNumber to it.year }
            .values
            .sumOf { rows -> rows.first().netAmount }
        val diesel = dieselDao.getDieselSince(minDate, maxDate)
        return PeriodFinance(
            paycheckTotal = paycheckTotal,
            dieselTotal = diesel.sumOf { it.totalAmount },
            dieselGallons = diesel.sumOf { it.gallons ?: 0.0 },
            dieselSavings = diesel.sumOf {
                DieselPurchaseMath.savings(
                    gallons = it.gallons,
                    pricePerGallon = it.pricePerGallon,
                    discountPricePerGallon = it.discountPricePerGallon,
                ) ?: 0.0
            },
        )
    }

    suspend fun getLoadsForWeek(weekNumber: Int, year: Int): List<Load> {
        val entities = loadDao.getLoadsByWeekOnce(weekNumber, year)
        if (entities.isEmpty()) return emptyList()
        val loadIds = entities.map { it.id }
        val stopsByLoadId = loadIds.chunked(500)
            .flatMap { chunk -> stopDao.getStopsByLoadIds(chunk) }
            .groupBy { it.loadId }
        return entities.map { entity ->
            entity.toDomain(stops = stopsByLoadId[entity.id].orEmpty())
        }
    }

    private suspend fun loadTopRoutes(minDate: String, maxDate: String, limit: Int): List<RouteData> {
        val entities = loadDao.getLoadsSince(minDate, maxDate)
        if (entities.isEmpty()) return emptyList()
        val loadIds = entities.map { it.id }
        val stopsByLoadId = loadIds.chunked(500)
            .flatMap { chunk -> stopDao.getStopsByLoadIds(chunk) }
            .groupBy { it.loadId }
        val loads = entities.map { entity ->
            entity.toDomain(stops = stopsByLoadId[entity.id].orEmpty())
        }
        return RouteDisplayHelper.topRoutes(loads, limit)
    }

    private fun weekSlotsFor(
        filter: AnalyticsFilter,
        weekRows: List<com.truckerload.data.local.entities.analytics.WeeklyRevenueAgg>,
    ): List<Pair<Int, Int>> = filter.weekSlots()
        ?: weekRows
            .map { it.weekNumber to it.year }
            .distinct()
            .sortedWith(compareBy<Pair<Int, Int>> { it.second }.thenBy { it.first })

    private fun mapDailyDistribution(rows: List<com.truckerload.data.local.entities.analytics.DailyGrossAgg>): List<DailyData> {
        val byDay = rows.associateBy { it.dayOfWeek }
        val order = listOf(0, 1, 2, 3, 4, 5, 6)
        val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return order.mapIndexed { index, dow ->
            val row = byDay[dow]
            DailyData(
                dayLabel = labels[index],
                dayOfWeek = dow,
                gross = row?.gross ?: 0.0,
                loadCount = row?.loadCount ?: 0,
            )
        }
    }
}

data class AnalyticsDashboard(
    val weeks: List<WeekData>,
    val routes: List<RouteData>,
    val daily: List<DailyData>,
    val summary: AnalyticsSummary,
    val finance: PeriodFinance = PeriodFinance(),
)
