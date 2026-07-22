package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.domain.analytics.RouteDisplayHelper
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.domain.model.analytics.AnalyticsSummary
import com.truckerload.domain.model.analytics.DailyData
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.domain.model.analytics.WeekData
import com.truckerload.utils.enumerateRecentWeekSlots
import com.truckerload.utils.getWeekRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnalyticsRepository(private val db: AppDatabase) {

    private val loadDao = db.loadDao()
    private val stopDao = db.stopDao()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun loadDashboard(period: AnalyticsPeriod): AnalyticsDashboard {
        val minDate = minDateFor(period)
        val weekRows = loadDao.getWeeklyRevenue(minDate)
        val dataByKey = weekRows.associateBy { it.weekNumber to it.year }
        val slots = weekSlotsFor(period, weekRows)
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
        val routes = loadTopRoutes(minDate, 5)
        val daily = mapDailyDistribution(loadDao.getDailyDistribution(minDate))
        val totals = loadDao.getAnalyticsTotals(minDate)
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

    private suspend fun loadTopRoutes(minDate: String, limit: Int): List<RouteData> {
        val entities = loadDao.getLoadsSince(minDate)
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
        period: AnalyticsPeriod,
        weekRows: List<com.truckerload.data.local.entities.analytics.WeeklyRevenueAgg>,
    ): List<Pair<Int, Int>> = when (period) {
        AnalyticsPeriod.LAST_12_WEEKS -> enumerateRecentWeekSlots(12)
        AnalyticsPeriod.LAST_6_MONTHS -> enumerateRecentWeekSlots(26)
        AnalyticsPeriod.ALL_TIME -> weekRows
            .map { it.weekNumber to it.year }
            .distinct()
            .sortedWith(compareBy<Pair<Int, Int>> { it.second }.thenBy { it.first })
    }

    private fun minDateFor(period: AnalyticsPeriod): String = when (period) {
        AnalyticsPeriod.LAST_12_WEEKS -> {
            val (firstWeek, firstYear) = enumerateRecentWeekSlots(12).first()
            getWeekRange(firstWeek, firstYear).first
        }
        AnalyticsPeriod.LAST_6_MONTHS -> LocalDate.now().minusMonths(6).format(dateFormatter)
        AnalyticsPeriod.ALL_TIME -> ""
    }

    private fun mapDailyDistribution(rows: List<com.truckerload.data.local.entities.analytics.DailyGrossAgg>): List<DailyData> {
        val byDay = rows.associateBy { it.dayOfWeek }
        val order = listOf(0, 1, 2, 3, 4, 5, 6)
        val labels = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
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
)
