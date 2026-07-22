package com.truckerload.presentation.screens.map

import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.StateRating
import com.truckerload.presentation.components.getUsStateCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStateMetricsTest {

    @Test
    fun emptyLoads_allStatesNoData() {
        val metrics = MapStateMetrics.computeFromLoads(emptyList())
        assertEquals(getUsStateCodes().size, metrics.size)
        assertTrue(metrics.all { it.trips == 0 && it.rating == StateRating.NO_DATA })
        assertTrue(metrics.all { it.revenue == 0.0 && it.revenuePerMile == 0.0 })
    }

    @Test
    fun singleLoad_marksDestinationState() {
        val load = Load(
            id = "1",
            tripId = "T-1",
            date = "2026-07-22",
            totalRate = 2500.0,
            totalMiles = 1000.0,
            pointA = "Hopewell Junction, NY",
            pointB = "Garner, NC",
            puCount = 1,
            delCount = 1,
            weekNumber = 30,
            year = 2026,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
        )
        val metrics = MapStateMetrics.computeFromLoads(listOf(load))
        val nc = metrics.first { it.code == "NC" }
        assertEquals(1, nc.trips)
        assertEquals(2500.0, nc.revenue, 0.01)
        assertEquals(2.5, nc.revenuePerMile, 0.01)
    }
}
