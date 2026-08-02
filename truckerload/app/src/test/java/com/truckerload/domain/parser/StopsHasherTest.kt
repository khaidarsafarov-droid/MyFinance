package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopsHasherTest {

    @Test
    fun `same stops produce same hash`() {
        val stops = listOf(sampleStop(1, StopType.PU), sampleStop(2, StopType.DEL))
        val hash1 = StopsHasher.calculateStopsHash(stops)
        val hash2 = StopsHasher.calculateStopsHash(stops)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun `different city produces different hash`() {
        val stopsA = listOf(sampleStop(1, StopType.PU, city = "Austin"))
        val stopsB = listOf(sampleStop(1, StopType.PU, city = "Dallas"))
        assertNotEquals(
            StopsHasher.calculateStopsHash(stopsA),
            StopsHasher.calculateStopsHash(stopsB),
        )
    }

    @Test
    fun `list order of same stopNumbers does not affect hash`() {
        val pu = sampleStop(1, StopType.PU)
        val del = sampleStop(2, StopType.DEL)
        assertEquals(
            StopsHasher.calculateStopsHash(listOf(pu, del)),
            StopsHasher.calculateStopsHash(listOf(del, pu)),
        )
    }

    @Test
    fun `route reorder with different stopNumbers changes hash`() {
        val first = listOf(
            sampleStop(1, StopType.PU, city = "Austin"),
            sampleStop(2, StopType.DEL, city = "Dallas"),
        )
        val reordered = listOf(
            sampleStop(1, StopType.PU, city = "Dallas"),
            sampleStop(2, StopType.DEL, city = "Austin"),
        )
        assertNotEquals(
            StopsHasher.calculateStopsHash(first),
            StopsHasher.calculateStopsHash(reordered),
        )
    }

    private fun sampleStop(
        number: Int,
        type: StopType,
        city: String = "Garner",
    ) = Stop(
        id = number,
        loadId = "load-1",
        stopNumber = number,
        type = type,
        puNumber = if (type == StopType.PU) "PU$number" else null,
        note = null,
        scheduledTime = "2026-06-10 08:00",
        timezone = "EDT",
        facilityCode = "TOL3",
        fullAddress = "$city, NC",
        city = city,
        state = "NC",
        zip = "27529",
    )
}
