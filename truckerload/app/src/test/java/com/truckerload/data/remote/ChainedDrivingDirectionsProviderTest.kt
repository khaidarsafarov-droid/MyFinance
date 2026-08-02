package com.truckerload.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChainedDrivingDirectionsProviderTest {

    private class StubProvider(
        override val providerLabel: String,
        private val configured: Boolean,
        private val points: List<com.truckerload.domain.friends.LatLngPoint>?,
        private val fail: Boolean = false,
    ) : com.truckerload.domain.friends.DrivingDirectionsProvider {
        override fun isConfigured(): Boolean = configured
        override suspend fun fetchDrivingRoute(
            origin: com.truckerload.domain.friends.LatLngPoint,
            destination: com.truckerload.domain.friends.LatLngPoint,
            options: com.truckerload.domain.friends.DrivingRouteOptions,
        ): Result<com.truckerload.domain.friends.DrivingRouteResult> {
            if (fail || points == null) {
                return Result.failure(IllegalStateException("fail $providerLabel"))
            }
            return Result.success(
                com.truckerload.domain.friends.DrivingRouteResult(points, providerLabel),
            )
        }
    }

    @Test
    fun triesNextProviderWhenFirstFails() = kotlinx.coroutines.runBlocking {
        val road = listOf(
            com.truckerload.domain.friends.LatLngPoint(41.0, -100.0),
            com.truckerload.domain.friends.LatLngPoint(41.0, -98.0),
        )
        val chain = ChainedDrivingDirectionsProvider(
            listOf(
                StubProvider("Google", configured = true, points = null, fail = true),
                StubProvider("OSRM", configured = true, points = road),
            ),
        )
        val result = chain.fetchDrivingRoute(
            road.first(),
            road.last(),
        )
        assertTrue(result.isSuccess)
        assertEquals("OSRM", result.getOrNull()!!.providerLabel)
        assertEquals(2, result.getOrNull()!!.points.size)
    }
}
