package com.truckerload.domain.parser

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LoadProcessorReplacedTest {

    private val loadRepository = mock<LoadRepository>()
    private val processor = LoadProcessor(loadRepository)

    @Test
    fun duplicateRoute_newTripId_returnsReplaced() = runBlocking {
        val existing = sampleLoad(tripId = "T-OLD", rate = 1500.0)
        val incoming = sampleLoad(tripId = "T-NEW", rate = 1500.0)
        whenever(loadRepository.getByTripId("T-NEW")).thenReturn(null)
        whenever(loadRepository.getByRouteAndDate(any(), any(), any(), any())).thenReturn(existing)

        val result = processor.processLoad(incoming, playFeedback = false)

        assertTrue(result is ProcessingResult.Replaced)
        assertEquals("tripId: T-OLD → T-NEW", (result as ProcessingResult.Replaced).reason)
        verify(loadRepository).update(any())
    }

    private fun sampleLoad(tripId: String, rate: Double): Load {
        val stops = listOf(
            Stop(
                id = 1,
                loadId = "id",
                stopNumber = 1,
                type = StopType.PU,
                puNumber = null,
                note = null,
                scheduledTime = "2026-08-01 08:00",
                timezone = "America/New_York",
                facilityCode = "SWF2",
                fullAddress = "SWF2, Garner, NC",
                city = "Garner",
                state = "NC",
                zip = "27529",
            ),
            Stop(
                id = 2,
                loadId = "id",
                stopNumber = 2,
                type = StopType.DEL,
                puNumber = null,
                note = null,
                scheduledTime = "2026-08-02 18:00",
                timezone = "America/Chicago",
                facilityCode = "DFW1",
                fullAddress = "DFW1, Dallas, TX",
                city = "Dallas",
                state = "TX",
                zip = "75201",
            ),
        )
        return Load(
            id = tripId,
            tripId = tripId,
            date = "2026-08-01",
            totalRate = rate,
            totalMiles = 850.0,
            pointA = "Garner, NC",
            pointB = "Dallas, TX",
            puCount = 1,
            delCount = 1,
            weekNumber = 31,
            year = 2026,
            rawMessage = "sample",
            parsedAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L,
            firstPuCityState = "Garner, NC",
            lastDelCityState = "Dallas, TX",
            stops = stops,
        )
    }
}
