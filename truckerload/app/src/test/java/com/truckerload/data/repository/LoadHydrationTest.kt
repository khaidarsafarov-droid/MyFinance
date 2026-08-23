package com.truckerload.data.repository

import com.truckerload.data.local.dao.PenaltyDao
import com.truckerload.data.local.dao.StopDao
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.StopEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class LoadHydrationTest {

    @Test
    fun hydrateLoadEntities_keepsStoredDateWithoutRepair() = runBlocking {
        val stopDao = mock<StopDao>()
        val penaltyDao = mock<PenaltyDao>()
        val entity = LoadEntity(
            id = "load-1",
            tripId = "T-116KYL6KW",
            date = "2026-07-05",
            totalRate = 2500.0,
            totalMiles = 850.0,
            pointA = "Garner, NC",
            pointB = "Atlanta, GA",
            puCount = 1,
            delCount = 1,
            weekNumber = 27,
            year = 2026,
            rawMessage = "Trip ID: T-116KYL6KW",
            parsedAt = 1_751_709_600_000L, // 2025-07-05-ish
            updatedAt = 1_751_709_600_000L,
        )
        whenever(stopDao.getStopsByLoadIds(any())).thenReturn(
            listOf(
                StopEntity(
                    id = 1,
                    loadId = entity.id,
                    stopNumber = 1,
                    type = "PU",
                    puNumber = "PU1",
                    note = "",
                    scheduledTime = "07/05 08:00 EDT",
                    timezone = "America/New_York",
                    facilityCode = "SWF2",
                    fullAddress = "123 Main",
                    city = "Garner",
                    state = "NC",
                    zip = "27529",
                ),
            ),
        )
        whenever(penaltyDao.getPenaltiesByLoadIds(any())).thenReturn(emptyList())

        val loads = hydrateLoadEntities(listOf(entity), stopDao, penaltyDao)

        assertEquals(1, loads.size)
        assertEquals("2026-07-05", loads.single().date)
        assertEquals(27, loads.single().weekNumber)
        assertEquals(2026, loads.single().year)
        assertEquals("07/05 08:00 EDT", loads.single().stops.single().scheduledTime)
    }

    @Test
    fun hydrateLoadEntities_emptyList_shortCircuits() = runBlocking {
        val stopDao = mock<StopDao>()
        val penaltyDao = mock<PenaltyDao>()
        val loads = hydrateLoadEntities(emptyList(), stopDao, penaltyDao)
        assertTrue(loads.isEmpty())
    }
}
