package com.truckerload.domain.import

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportTripDedupTest {

    @Test
    fun keepLatestByTripId_prefersNewestRevision() {
        val older = sampleLoad("T-ABC", rate = 1000.0)
        val newer = sampleLoad("T-ABC", rate = 2500.0)
        val other = sampleLoad("T-XYZ", rate = 900.0)

        val result = ImportTripDedup.keepLatestByTripId(listOf(older, other, newer))

        assertEquals(listOf("T-XYZ", "T-ABC"), result.map { it.tripId })
        assertEquals(2500.0, result.first { it.tripId == "T-ABC" }.totalRate, 0.0)
    }

    @Test
    fun keepLatestByTripId_isCaseInsensitive() {
        val older = sampleLoad("t-abc", rate = 1.0)
        val newer = sampleLoad("T-ABC", rate = 2.0)
        val result = ImportTripDedup.keepLatestByTripId(listOf(older, newer))
        assertEquals(1, result.size)
        assertEquals(2.0, result.single().totalRate, 0.0)
    }

    private fun sampleLoad(tripId: String, rate: Double) = Load(
        id = tripId,
        tripId = tripId,
        date = "2026-07-01",
        totalRate = rate,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 1,
        year = 2026,
        rawMessage = "",
        parsedAt = 0L,
        updatedAt = 0L,
    )
}
