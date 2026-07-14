package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class DuplicateAuditUseCaseTest {

    private val useCase = DuplicateAuditUseCase(
        loadRepository = mock(),
        paycheckRepository = mock(),
        dieselRepository = mock(),
    )

    @Test
    fun `case-insensitive tripId duplicates are removed`() {
        val loads = listOf(
            sampleLoad(id = "1", tripId = "ABC123", parsedAt = 10L),
            sampleLoad(id = "2", tripId = "abc123", parsedAt = 20L),
        )

        val deleted = useCase.findDuplicateLoadIds(loads)

        assertEquals(setOf("2"), deleted)
    }

    @Test
    fun `identical stops and date are treated as duplicates`() {
        val keeper = sampleLoad(id = "1", tripId = "T-ONE", parsedAt = 10L)
        val duplicate = sampleLoad(id = "2", tripId = "T-TWO", parsedAt = 20L)

        val deleted = useCase.findDuplicateLoadIds(listOf(keeper, duplicate))

        assertEquals(setOf("2"), deleted)
    }

    @Test
    fun `different loads with same route but different rate are kept`() {
        val first = sampleLoad(id = "1", tripId = "T-ONE", parsedAt = 10L, totalRate = 1000.0)
        val second = sampleLoad(
            id = "2",
            tripId = "T-TWO",
            parsedAt = 20L,
            totalRate = 2000.0,
        )

        val deleted = useCase.findDuplicateLoadIds(listOf(first, second))

        assertTrue(deleted.isEmpty())
    }

    @Test
    fun `duplicate paycheck weeks keep the newest entry`() {
        val paychecks = listOf(
            paycheck(id = 1, addedAt = 100L),
            paycheck(id = 2, addedAt = 200L),
        )

        val deleted = useCase.findDuplicatePaycheckIds(paychecks)

        assertEquals(setOf(1), deleted)
    }

    private fun sampleLoad(
        id: String,
        tripId: String,
        parsedAt: Long,
        totalRate: Double = 2500.0,
    ) = Load(
        id = id,
        tripId = tripId,
        date = "2026-06-10",
        totalRate = totalRate,
        totalMiles = 850.0,
        pointA = "Hopewell Junction, NY",
        pointB = "Garner, NC",
        puCount = 1,
        delCount = 1,
        weekNumber = 24,
        year = 2026,
        rawMessage = "Trip ID: $tripId",
        parsedAt = parsedAt,
        updatedAt = parsedAt,
        stopCount = 2,
        firstPuCityState = "Hopewell Junction, NY",
        lastDelCityState = "Garner, NC",
        stops = listOf(
            stop(1, StopType.PU, "Hopewell Junction"),
            stop(2, StopType.DEL, "Garner"),
        ),
    )

    private fun stop(number: Int, type: StopType, city: String) = Stop(
        id = number,
        loadId = "load-1",
        stopNumber = number,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = "2026-06-10 08:00",
        timezone = "America/New_York",
        facilityCode = null,
        fullAddress = "$city, ${if (type == StopType.PU) "NY" else "NC"}",
        city = city,
        state = if (type == StopType.PU) "NY" else "NC",
        zip = if (type == StopType.PU) "12533" else "27529",
    )

    private fun paycheck(id: Int, addedAt: Long) =
        com.truckerload.domain.model.Paycheck(
            id = id,
            weekNumber = 24,
            year = 2026,
            weekLabel = "W24",
            weekStartDate = "2026-06-09",
            weekEndDate = "2026-06-15",
            driverName = "Test",
            grossAmount = 3000.0,
            netAmount = 2500.0,
            rawExtractedText = "settlement",
            sourceFileName = null,
            addedAt = addedAt,
        )
}
