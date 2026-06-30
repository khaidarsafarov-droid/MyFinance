package com.truckerload.domain.analytics

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDisplayHelperTest {

    @Test
    fun `warehouse codes are rejected`() {
        assertTrue(RouteDisplayHelper.isWarehouseCode("TOL3"))
        assertTrue(RouteDisplayHelper.isWarehouseCode("RDU1"))
        assertFalse(RouteDisplayHelper.isWarehouseCode("Hopewell Junction, NY"))
    }

    @Test
    fun `top routes group by city state not terminal codes`() {
        val loads = listOf(
            testLoad(
                pointA = "TOL3",
                pointB = "RDU1",
                stops = listOf(
                    testStop(1, StopType.PU, "Hopewell Junction", "NY"),
                    testStop(2, StopType.DEL, "Garner", "NC"),
                ),
                rate = 2500.0,
                miles = 500.0,
            ),
            testLoad(
                pointA = "AKR1",
                pointB = "TOL3",
                stops = listOf(
                    testStop(1, StopType.PU, "Hopewell Junction", "NY"),
                    testStop(2, StopType.DEL, "Garner", "NC"),
                ),
                rate = 1800.0,
                miles = 480.0,
            ),
        )

        val routes = RouteDisplayHelper.topRoutes(loads, 5)
        assertEquals(1, routes.size)
        assertEquals("Hopewell Junction, NY", routes[0].origin)
        assertEquals("Garner, NC", routes[0].destination)
        assertEquals(2, routes[0].loadCount)
        assertEquals(4300.0, routes[0].gross, 0.01)
    }

    private fun testLoad(
        pointA: String,
        pointB: String,
        stops: List<Stop>,
        rate: Double,
        miles: Double,
    ) = Load(
        id = "id-$pointA-$rate",
        tripId = "T-$pointA",
        date = "2026-06-01",
        totalRate = rate,
        totalMiles = miles,
        pointA = pointA,
        pointB = pointB,
        puCount = 1,
        delCount = 1,
        weekNumber = 23,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        stops = stops,
    )

    private fun testStop(num: Int, type: StopType, city: String, state: String) = Stop(
        id = num,
        loadId = "load",
        stopNumber = num,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = "",
        timezone = "UTC",
        facilityCode = null,
        fullAddress = "$city, $state",
        city = city,
        state = state,
        zip = "",
    )
}
