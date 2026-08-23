package com.truckerload.domain.parser

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class DuplicateCheckerTest {

    private val checker = DuplicateChecker(loadRepository = mock<LoadRepository>())

    @Test
    fun sameRouteAndDate_differentRate_isNotSameLoad() {
        val existing = sampleLoad(tripId = "T-1", rate = 1200.0)
        val incoming = sampleLoad(tripId = "T-2", rate = 1800.0)
        assertFalse(checker.isLikelySameLoad(existing, incoming))
    }

    @Test
    fun identicalLoads_areSameLoad() {
        val existing = sampleLoad(tripId = "T-1", rate = 1500.0)
        val incoming = sampleLoad(tripId = "T-1-COPY", rate = 1500.0)
        assertTrue(checker.isLikelySameLoad(existing, incoming))
    }

    @Test
    fun sameTripId_isSameLoad() {
        val existing = sampleLoad(tripId = "T-99", rate = 100.0)
        val incoming = sampleLoad(tripId = "t-99", rate = 9999.0)
        assertTrue(checker.isLikelySameLoad(existing, incoming))
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
            id = "id-$tripId",
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
            parsedAt = 1L,
            updatedAt = 1L,
            firstPuCityState = "Garner, NC",
            lastDelCityState = "Dallas, TX",
            stops = stops,
        )
    }
}
