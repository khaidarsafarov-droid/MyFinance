package com.truckerload.domain.store

import com.truckerload.domain.money.Money
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.WeekSummary

class FakeLoadStore : LoadStore {
    val saved = mutableListOf<Load>()
    val updated = mutableListOf<Load>()
    val history = mutableListOf<LoadFieldChange>()
    val deleted = mutableListOf<String>()
    var refreshCount = 0

    override suspend fun getByTripId(tripId: String): Load? =
        saved.find { it.tripId.equals(tripId, ignoreCase = true) }

    override suspend fun getByRouteAndDate(
        origin: String,
        destination: String,
        date: String,
        rate: Money,
    ): Load? = saved.find { load ->
        load.date == date &&
            load.totalRate == rate &&
            load.firstPuCityState.ifBlank { load.pointA } == origin &&
            load.lastDelCityState.ifBlank { load.pointB } == destination
    }

    override suspend fun getByStops(stops: List<Stop>, date: String): Load? = null

    override suspend fun insertLoad(load: Load, playFeedback: Boolean, reviveDeleted: Boolean) {
        saved += load
    }

    override suspend fun update(load: Load) {
        updated += load
    }

    override suspend fun addChangeHistory(change: LoadFieldChange) {
        history += change
    }

    override suspend fun <T> runBatchWrite(block: suspend () -> T): T = block()

    override suspend fun refreshReportingWeeks() {
        refreshCount += 1
    }

    override suspend fun getAllLoadsOnce(): List<Load> = saved.toList()

    override suspend fun deleteLoad(loadId: String) {
        deleted += loadId
        saved.removeAll { it.id == loadId }
    }

    override suspend fun loadsForWeek(weekNumber: Int, year: Int): List<Load> =
        saved.filter { it.weekNumber == weekNumber && it.year == year }
}

class FakeDieselStore : DieselStore {
    val byWeek = mutableMapOf<Pair<Int, Int>, List<Diesel>>()
    val inserted = mutableListOf<Diesel>()
    var replacedWeek: List<Diesel>? = null
    val deleted = mutableListOf<Int>()
    var all: List<Diesel> = emptyList()
    var reportingReplacements: List<Diesel> = emptyList()

    override suspend fun getDieselForWeekOnce(weekNumber: Int, year: Int): List<Diesel> =
        byWeek[weekNumber to year] ?: all.filter { it.weekNumber == weekNumber && it.year == year }

    override suspend fun insertDiesel(diesel: Diesel) {
        inserted += diesel
    }

    override suspend fun replaceWeekFills(weekNumber: Int, year: Int, fills: List<Diesel>) {
        replacedWeek = fills
        byWeek[weekNumber to year] = fills
    }

    override suspend fun getAllDieselOnce(): List<Diesel> = all

    override suspend fun replaceReportingWeeks(updated: List<Diesel>) {
        reportingReplacements = updated
    }

    override suspend fun deleteDiesel(id: Int) {
        deleted += id
    }
}

class FakePaycheckStore : PaycheckStore {
    var all: List<Paycheck> = emptyList()
    var reportingReplacements: List<Paycheck> = emptyList()
    val deleted = mutableListOf<Int>()

    override suspend fun getAllPaychecksOnce(): List<Paycheck> = all

    override suspend fun replaceReportingWeeks(updated: List<Paycheck>) {
        reportingReplacements = updated
    }

    override suspend fun deletePaycheck(id: Int) {
        deleted += id
    }
}

class FakeWeekSummaryStore(
    private val summaries: Map<Pair<Int, Int>, WeekSummary> = emptyMap(),
) : WeekSummaryStore {
    override suspend fun getWeekSummaryOnce(weekNumber: Int, year: Int): WeekSummary =
        summaries[weekNumber to year] ?: emptySummary(weekNumber, year)

    private fun emptySummary(weekNumber: Int, year: Int) = WeekSummary(
        weekNumber = weekNumber,
        year = year,
        weekLabel = "",
        weekStartDate = "",
        weekEndDate = "",
        loadsCount = 0,
        totalLoadRate = 0.0,
        totalMiles = 0.0,
        paycheckAmount = 0.0,
        hasPaycheck = false,
        dieselAmount = 0.0,
        hasDiesel = false,
        netProfit = 0.0,
    )
}
