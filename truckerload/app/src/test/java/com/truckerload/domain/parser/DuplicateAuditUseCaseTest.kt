package com.truckerload.domain.parser

import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

class DuplicateAuditUseCaseTest {

    private val loadRepository: LoadRepository = mock()
    private val paycheckRepository: PaycheckRepository = mock()
    private val dieselRepository: DieselRepository = mock()
    private val useCase = DuplicateAuditUseCase(
        loadRepository = loadRepository,
        paycheckRepository = paycheckRepository,
        dieselRepository = dieselRepository,
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

    @Test
    fun `auditAndRemove after import deletes newer tripId duplicate`() = runBlocking {
        val keeper = sampleLoad(id = "1", tripId = "T-IMPORT", parsedAt = 10L)
        val dup = sampleLoad(id = "2", tripId = "t-import", parsedAt = 99L)
        val deleted = mutableListOf<String>()
        whenever(loadRepository.getAllLoadsOnce()).thenReturn(listOf(keeper, dup))
        whenever(paycheckRepository.getAllPaychecksOnce()).thenReturn(emptyList())
        whenever(dieselRepository.getAllDieselOnce()).thenReturn(emptyList())
        loadRepository.stub {
            onBlocking { deleteLoad(any()) } doAnswer { inv: org.mockito.invocation.InvocationOnMock ->
                deleted += inv.getArgument<String>(0)
                Unit
            }
        }

        val report = useCase.auditAndRemove()

        assertEquals(2, report.scannedLoads)
        assertEquals(1, report.deletedLoads)
        assertEquals(listOf("t-import"), report.deletedLoadTripIds)
        assertEquals(listOf("2"), deleted)
    }

    @Test
    fun `auditAndRemove with no duplicates returns zeros`() = runBlocking {
        whenever(loadRepository.getAllLoadsOnce()).thenReturn(
            listOf(sampleLoad(id = "1", tripId = "T-ONLY", parsedAt = 1L)),
        )
        whenever(paycheckRepository.getAllPaychecksOnce()).thenReturn(emptyList())
        whenever(dieselRepository.getAllDieselOnce()).thenReturn(emptyList())

        val report = useCase.auditAndRemove()

        assertEquals(1, report.scannedLoads)
        assertEquals(0, report.deletedLoads)
        assertEquals(0, report.deletedPaychecks)
        assertEquals(0, report.deletedDiesel)
        assertTrue(report.deletedLoadTripIds.isEmpty())
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
