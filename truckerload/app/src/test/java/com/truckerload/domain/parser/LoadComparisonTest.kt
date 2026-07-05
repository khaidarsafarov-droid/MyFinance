package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadComparisonTest {

    @Test
    fun `identical loads are skipped`() {
        val load = sampleLoad(totalRate = 2500.0, totalMiles = 850.0)
        val comparison = compareLoads(load, load.copy(rawMessage = "different raw"))
        assertTrue(comparison.isIdentical())
        assertFalse(comparison.hasMinorChanges())
        assertFalse(comparison.hasMajorChanges())
    }

    @Test
    fun `rate change only is minor`() {
        val old = sampleLoad(totalRate = 2500.0)
        val newLoad = old.copy(totalRate = 2600.0)
        val comparison = compareLoads(old, newLoad)
        assertFalse(comparison.isIdentical())
        assertTrue(comparison.hasMinorChanges())
        assertFalse(comparison.hasMajorChanges())
    }

    @Test
    fun `stop count change is major`() {
        val old = sampleLoad()
        val newLoad = old.copy(
            stops = old.stops + sampleStop(3, StopType.DEL, "Charlotte"),
        )
        val comparison = compareLoads(old, newLoad)
        assertFalse(comparison.isIdentical())
        assertFalse(comparison.hasMinorChanges())
        assertTrue(comparison.hasMajorChanges())
    }

    @Test
    fun `small rate change within threshold is identical`() {
        val old = sampleLoad(totalRate = 1000.0)
        val newLoad = old.copy(totalRate = 1005.0)
        val comparison = compareLoads(old, newLoad, priceThresholdPercent = 1.0)
        assertTrue(comparison.isIdentical())
    }

    private fun sampleLoad(
        totalRate: Double = 2500.0,
        totalMiles: Double = 850.0,
    ) = Load(
        id = "load-1",
        tripId = "T-TEST123",
        date = "2026-06-10",
        totalRate = totalRate,
        totalMiles = totalMiles,
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
            sampleStop(1, StopType.PU, "Hopewell Junction"),
            sampleStop(2, StopType.DEL, "Garner"),
        ),
    )

    private fun sampleStop(number: Int, type: StopType, city: String) = Stop(
        id = number,
        loadId = "load-1",
        stopNumber = number,
        type = type,
        puNumber = if (type == StopType.PU) "PU$number" else null,
        note = null,
        scheduledTime = "2026-06-10 08:00",
        timezone = "EDT",
        facilityCode = "FAC$number",
        fullAddress = "$city, NC",
        city = city,
        state = "NC",
        zip = "27529",
    )
}
