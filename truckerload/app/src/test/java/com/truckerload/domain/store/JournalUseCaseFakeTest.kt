package com.truckerload.domain.store

import com.truckerload.domain.importing.DieselImportAction
import com.truckerload.domain.importing.DieselImportUseCase
import com.truckerload.domain.importing.DieselSpreadsheetImport
import com.truckerload.domain.importing.ParsedDieselFill
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.WeekSummary
import com.truckerload.domain.money.Money
import com.truckerload.domain.parser.DuplicateChecker
import com.truckerload.domain.parser.DuplicateResult
import com.truckerload.domain.parser.LoadProcessor
import com.truckerload.domain.parser.ProcessingResult
import com.truckerload.domain.usecase.ForecastService
import com.truckerload.domain.usecase.ForecastTrend
import com.truckerload.domain.week.WeekStartRebinder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalUseCaseFakeTest {

    @Test
    fun processorSkipsADeletedTripAndInsertsANewOne() = runBlocking {
        val loads = FakeLoadStore()
        val blocked = DeletedLoadLookup { _, tripId -> tripId == "T-GONE" }
        val processor = LoadProcessor(loads, blocked)

        val skipped = processor.processLoad(sample("T-GONE"), playFeedback = false)
        val added = processor.processLoad(sample("T-NEW"), playFeedback = false)

        assertTrue(skipped is ProcessingResult.Skipped)
        assertEquals("Deleted", (skipped as ProcessingResult.Skipped).reason)
        assertTrue(added is ProcessingResult.Added)
        assertEquals(listOf("T-NEW"), loads.saved.map { it.tripId })
    }

    @Test
    fun duplicateCheckerFindsTheStoredTrip() = runBlocking {
        val loads = FakeLoadStore()
        val existing = sample("T-1")
        loads.saved += existing
        val result = DuplicateChecker(loads).checkDuplicate(sample("t-1"))
        assertTrue(result is DuplicateResult.Found)
        assertEquals(existing.id, (result as DuplicateResult.Found).load.id)
    }

    @Test
    fun dieselImportAddsFillsThroughTheStore() = runBlocking {
        val diesel = FakeDieselStore()
        diesel.all = listOf(sampleDiesel())
        val useCase = DieselImportUseCase(diesel)
        val review = useCase.buildReview(
            DieselSpreadsheetImport(
                fileName = "fuel.xlsx",
                driverName = "Sam",
                weekStartDate = "2026-03-02",
                weekEndDate = "2026-03-08",
                weekNumber = 10,
                year = 2026,
                fills = listOf(
                    ParsedDieselFill(
                        transactionDate = "2026-03-03",
                        location = "Pilot",
                        gallons = 50.0,
                        pricePerGallon = 4.0,
                        totalAmount = 200.0,
                    ),
                ),
            ),
        )
        assertTrue(review.hasConflict)
        assertEquals(1, review.comparison.existingCount)
        useCase.apply(review, DieselImportAction.ADD_FROM_FILE)
        assertEquals(1, diesel.inserted.size)
        assertEquals(200.0, diesel.inserted.single().totalAmount.cents / 100.0, 0.01)
    }

    @Test
    fun weekRebindAsksEachStoreToRefresh() = runBlocking {
        val loads = FakeLoadStore()
        val diesel = FakeDieselStore()
        val paychecks = FakePaycheckStore()
        WeekStartRebinder.rebind(loads, diesel, paychecks)
        assertEquals(1, loads.refreshCount)
        assertTrue(diesel.reportingReplacements.isEmpty())
        assertTrue(paychecks.reportingReplacements.isEmpty())
    }

    @Test
    fun forecastUsesWeekSummariesAndCurrentLoads() = runBlocking {
        val weeks = FakeWeekSummaryStore(
            mapOf(
                9 to 2026 to summary(9, paycheck = 1000.0, loadRate = 800.0),
            ),
        )
        val loads = FakeLoadStore()
        loads.saved += sample("T-NOW").copy(weekNumber = 10, year = 2026, totalRate = Money.fromDollars(900.0))
        val forecast = ForecastService(weeks, loads).calculateForecast(10, 2026)
        requireNotNull(forecast)
        assertEquals(800.0, forecast.expectedRate, 0.01)
        assertEquals(900.0, forecast.currentRate, 0.01)
        assertEquals(ForecastTrend.ABOVE, forecast.trend)
        assertEquals(1, forecast.basedOnWeeks)
    }

    private fun sample(tripId: String) = Load(
        id = "id-$tripId",
        tripId = tripId,
        date = "2026-08-01",
        totalRate = Money.fromDollars(1500.0),
        totalMiles = 400.0,
        pointA = "Garner, NC",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = 31,
        year = 2026,
        rawMessage = "sample",
        parsedAt = 1L,
        updatedAt = 1L,
        stops = listOf(
            Stop(
                id = 1,
                loadId = "id-$tripId",
                stopNumber = 1,
                type = StopType.PU,
                puNumber = null,
                note = null,
                scheduledTime = "2026-08-01 08:00",
                timezone = "America/New_York",
                facilityCode = null,
                fullAddress = "Garner, NC",
                city = "Garner",
                state = "NC",
                zip = "27529",
            ),
        ),
    )

    private fun sampleDiesel() = Diesel(
        id = 1,
        weekNumber = 10,
        year = 2026,
        weekLabel = "W10",
        weekStartDate = "2026-03-02",
        weekEndDate = "2026-03-08",
        totalAmount = Money.fromDollars(100.0),
        gallons = 25.0,
        pricePerGallon = null,
        location = "TA",
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = 1L,
    )

    private fun summary(week: Int, paycheck: Double, loadRate: Double) = WeekSummary(
        weekNumber = week,
        year = 2026,
        weekLabel = "W$week",
        weekStartDate = "2026-03-02",
        weekEndDate = "2026-03-08",
        loadsCount = 1,
        totalLoadRate = loadRate,
        totalMiles = 100.0,
        paycheckAmount = paycheck,
        hasPaycheck = paycheck > 0,
        dieselAmount = 0.0,
        hasDiesel = false,
        netProfit = paycheck,
    )
}
