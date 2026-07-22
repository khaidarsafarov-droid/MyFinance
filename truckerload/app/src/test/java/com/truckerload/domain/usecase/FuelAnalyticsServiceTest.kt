package com.truckerload.domain.usecase

import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FuelAnalyticsServiceTest {

    @Test
    fun calculateForWeek_includesPreviousOnce_withoutInfiniteRecursion() = runTest {
        val dieselRepository = mock<DieselRepository>()
        val loadRepository = mock<LoadRepository>()
        whenever(dieselRepository.getDieselForWeek(any(), any())).thenReturn(
            flowOf(
                listOf(
                    Diesel(
                        id = 1,
                        weekNumber = 10,
                        year = 2026,
                        weekLabel = "W10",
                        weekStartDate = "2026-03-02",
                        weekEndDate = "2026-03-08",
                        totalAmount = 400.0,
                        gallons = 100.0,
                        pricePerGallon = 4.0,
                        location = null,
                        rawExtractedText = "",
                        sourceFileName = null,
                        addedAt = 1L,
                    ),
                ),
            ),
        )
        whenever(loadRepository.getLoadsByWeek(any(), any())).thenReturn(
            flowOf(
                listOf(
                    Load(
                        id = "l1",
                        tripId = "T1",
                        date = "2026-03-03",
                        totalRate = 2500.0,
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
                    ),
                ),
            ),
        )

        val service = FuelAnalyticsService(dieselRepository, loadRepository)
        val result = service.calculateForWeek(10, 2026)

        assertEquals(400.0, result.totalSpent, 0.01)
        assertEquals(100.0, result.totalGallons, 0.01)
        assertEquals(5.0, result.avgMpg, 0.01)
        assertNotNull(result.previousPeriod)
        assertNull(result.previousPeriod!!.previousPeriod)
    }
}
