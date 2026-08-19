package com.truckerload.domain.import

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadValidatorTest {

    private val validator = LoadValidator()

    @Test
    fun validate_acceptsLoadWithRoutePointsAndPositiveRate() {
        val result = validator.validate(sampleLoad(tripId = "T-ABC", totalRate = 2500.0))

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validate_rejectsShortTripId() {
        val result = validator.validate(sampleLoad(tripId = "AB", totalRate = 100.0))

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid Trip ID") })
    }

    @Test
    fun validate_rejectsNonPositiveRate() {
        val result = validator.validate(sampleLoad(tripId = "T-123", totalRate = 0.0))

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid rate") })
    }

    @Test
    fun validate_rejectsLoadWithoutRouteOrStops() {
        val result = validator.validate(
            sampleLoad(
                tripId = "T-123",
                totalRate = 500.0,
                pointA = "",
                pointB = "",
                stops = emptyList(),
            ),
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("No route points found") })
    }

    @Test
    fun validate_acceptsPuOnlyOrDelOnlyStops() {
        val puOnly = validator.validate(
            sampleLoad(
                tripId = "T-123",
                totalRate = 500.0,
                pointA = "",
                pointB = "",
                stops = listOf(sampleStop(type = StopType.PU)),
            ),
        )
        val delOnly = validator.validate(
            sampleLoad(
                tripId = "T-123",
                totalRate = 500.0,
                pointA = "",
                pointB = "",
                stops = listOf(sampleStop(type = StopType.DEL, stopNumber = 2)),
            ),
        )

        assertTrue(puOnly.isValid)
        assertTrue(delOnly.isValid)
    }

    private fun sampleLoad(
        tripId: String,
        totalRate: Double,
        pointA: String = "Atlanta, GA",
        pointB: String = "Denver, CO",
        stops: List<Stop> = emptyList(),
    ) = Load(
        id = "load-1",
        tripId = tripId,
        date = "2026-07-16",
        totalRate = totalRate,
        totalMiles = 850.0,
        pointA = pointA,
        pointB = pointB,
        puCount = 1,
        delCount = 1,
        weekNumber = 29,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        stops = stops,
    )

    private fun sampleStop(type: StopType, stopNumber: Int = 1) = Stop(
        id = stopNumber,
        loadId = "load-1",
        stopNumber = stopNumber,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = "2026-07-16 08:00",
        timezone = "America/New_York",
        facilityCode = "SWF2",
        fullAddress = "123 Main St",
        city = "Atlanta",
        state = "GA",
        zip = "30301",
    )
}
