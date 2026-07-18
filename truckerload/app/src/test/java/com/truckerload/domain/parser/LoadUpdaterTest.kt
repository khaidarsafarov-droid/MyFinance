package com.truckerload.domain.parser

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LoadUpdaterTest {

    @Test
    fun `updateLoad copies penalties from new data`() = runBlocking {
        val repository = mock<LoadRepository>()
        whenever(repository.update(org.mockito.kotlin.any())).thenAnswer { }
        whenever(repository.addChangeHistory(org.mockito.kotlin.any())).thenAnswer { }
        val updater = LoadUpdater(repository)

        val oldLoad = sampleLoad(
            penalties = listOf(Penalty(id = 1, loadId = "load-1", amount = 50.0, description = "late")),
        )
        val newLoad = oldLoad.copy(
            totalRate = 2600.0,
            penalties = listOf(
                Penalty(id = 0, loadId = "", amount = 100.0, description = "detention"),
                Penalty(id = 0, loadId = "", amount = 25.0, description = "lumper"),
            ),
        )

        updater.updateLoad(oldLoad, newLoad, listOf("totalRate: 2500.0 -> 2600.0"))

        val captor = argumentCaptor<Load>()
        verify(repository).update(captor.capture())
        val saved = captor.firstValue
        assertEquals(2, saved.penalties.size)
        assertEquals("load-1", saved.penalties[0].loadId)
        assertEquals(100.0, saved.penalties[0].amount, 0.001)
        assertEquals("detention", saved.penalties[0].description)
        assertEquals("lumper", saved.penalties[1].description)
    }

    @Test
    fun `updateLoad preserves load id when copying penalties`() = runBlocking {
        val repository = mock<LoadRepository>()
        whenever(repository.update(org.mockito.kotlin.any())).thenAnswer { }
        val updater = LoadUpdater(repository)

        val oldLoad = sampleLoad(id = "persist-id")
        val newLoad = oldLoad.copy(
            penalties = listOf(Penalty(id = 0, loadId = "wrong", amount = 75.0, description = "fee")),
        )

        updater.updateLoad(oldLoad, newLoad, emptyList())

        val captor = argumentCaptor<Load>()
        verify(repository).update(captor.capture())
        assertEquals("persist-id", captor.firstValue.penalties.single().loadId)
    }

    private fun sampleLoad(
        id: String = "load-1",
        penalties: List<Penalty> = emptyList(),
    ) = Load(
        id = id,
        tripId = "T-TEST123",
        date = "2026-06-10",
        totalRate = 2500.0,
        totalMiles = 850.0,
        pointA = "Hopewell Junction, NY",
        pointB = "Garner, NC",
        puCount = 1,
        delCount = 1,
        weekNumber = 24,
        year = 2026,
        rawMessage = "Trip ID: T-TEST123",
        parsedAt = 1L,
        updatedAt = 1L,
        stopCount = 2,
        stops = listOf(
            Stop(
                id = 1,
                loadId = id,
                stopNumber = 1,
                type = StopType.PU,
                puNumber = null,
                note = null,
                scheduledTime = "2026-06-10 08:00",
                timezone = "America/New_York",
                facilityCode = null,
                fullAddress = "Hopewell Junction, NY",
                city = "Hopewell Junction",
                state = "NY",
                zip = "12533",
            ),
        ),
        penalties = penalties,
    )
}
