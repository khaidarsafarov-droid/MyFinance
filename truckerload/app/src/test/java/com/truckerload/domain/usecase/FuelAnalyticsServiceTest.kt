package com.truckerload.domain.usecase

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice
import com.truckerload.domain.store.FakeDieselStore
import com.truckerload.domain.store.FakeLoadStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FuelAnalyticsServiceTest {

    @Test
    fun calculateForWeek_includesPreviousOnce_withoutInfiniteRecursion() = runTest {
        val diesel = FakeDieselStore()
        val loads = FakeLoadStore()
        diesel.all = listOf(
            Diesel(
                id = 1,
                weekNumber = 10,
                year = 2026,
                weekLabel = "W10",
                weekStartDate = "2026-03-02",
                weekEndDate = "2026-03-08",
                totalAmount = Money.fromDollars(400.0),
                gallons = 100.0,
                pricePerGallon = UnitPrice.fromDollars(4.0),
                discountPricePerGallon = UnitPrice.fromDollars(3.50),
                location = null,
                rawExtractedText = "",
                sourceFileName = null,
                addedAt = 1L,
            ),
        )
        loads.saved += Load(
            id = "l1",
            tripId = "T1",
            date = "2026-03-03",
            totalRate = Money.fromDollars(2500.0),
            totalMiles = 500.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 1,
            weekNumber = 10,
            year = 2026,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
        )

        val result = FuelAnalyticsService(diesel, loads).calculateForWeek(10, 2026)

        assertEquals(400.0, result.totalSpent, 0.01)
        assertEquals(100.0, result.totalGallons, 0.01)
        assertEquals(50.0, result.totalSavings, 0.01)
        assertEquals(5.0, result.avgMpg, 0.01)
        assertNotNull(result.previousPeriod)
        assertNull(result.previousPeriod!!.previousPeriod)
    }
}
