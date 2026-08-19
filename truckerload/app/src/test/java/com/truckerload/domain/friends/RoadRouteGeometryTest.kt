package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncodedPolylineCodecTest {

    @Test
    fun decodesGoogleSamplePolyline() {
        // Classic Google sample: "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = EncodedPolylineCodec.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].lat, 0.01)
        assertEquals(-120.2, points[0].lng, 0.01)
        assertEquals(40.7, points[1].lat, 0.01)
        assertEquals(-120.95, points[1].lng, 0.01)
        assertEquals(43.252, points[2].lat, 0.01)
        assertEquals(-126.453, points[2].lng, 0.01)
    }

    @Test
    fun blankReturnsEmpty() {
        assertTrue(EncodedPolylineCodec.decode("").isEmpty())
    }
}

class RoadRouteGeometryTest {

    private val i80Corridor = listOf(
        LatLngPoint(41.0, -100.0),
        LatLngPoint(41.0, -99.0),
        LatLngPoint(41.0, -98.0),
        LatLngPoint(41.0, -97.0),
    )

    @Test
    fun onRouteWhenNearPolylineWithin50m() {
        // ~22 m north of the corridor at lon -98.5
        val me = LatLngPoint(41.0002, -98.5)
        assertFalse(RoadRouteGeometry.isOffRoute(me, i80Corridor, thresholdMeters = 50.0))
    }

    @Test
    fun offRouteWhenBeyond50m() {
        // ~222 m north
        val me = LatLngPoint(41.002, -98.5)
        assertTrue(RoadRouteGeometry.isOffRoute(me, i80Corridor, thresholdMeters = 50.0))
    }

    @Test
    fun remainingStartsAtCurrentAndEndsAtDest() {
        val current = LatLngPoint(41.0, -98.4)
        val remaining = RoadRouteGeometry.remainingFromCurrent(i80Corridor, current)
        assertEquals(current.lat, remaining.first().lat, 1e-9)
        assertEquals(current.lng, remaining.first().lng, 1e-9)
        assertEquals(i80Corridor.last().lat, remaining.last().lat, 1e-9)
        assertTrue(remaining.size >= 2)
    }

    @Test
    fun traveledEndsAtCurrentAndStartsAtOrigin() {
        val current = LatLngPoint(41.0, -98.4)
        val traveled = RoadRouteGeometry.traveledToCurrent(i80Corridor, current)
        assertEquals(i80Corridor.first().lat, traveled.first().lat, 1e-9)
        assertEquals(current.lat, traveled.last().lat, 1e-9)
        assertEquals(current.lng, traveled.last().lng, 1e-9)
        assertTrue(traveled.size >= 2)
    }

    @Test
    fun defaultThresholdIsFiftyMeters() {
        assertEquals(50.0, RoadRouteGeometry.DEFAULT_OFF_ROUTE_METERS, 0.0)
    }
}

class RoadRouteSessionTest {

    private class FakeDirections(
        private var responses: MutableList<RoadRouteResult>,
        private val configured: Boolean = true,
    ) : DrivingDirectionsProvider {
        var fetchCount = 0
        override fun isConfigured(): Boolean = configured
        override suspend fun fetchRoute(request: RouteRequest): Result<RoadRouteResult> {
            fetchCount++
            val next = responses.removeFirstOrNull()
                ?: return Result.failure(IllegalStateException("no more fakes"))
            return Result.success(next)
        }
    }

    private fun road(vararg points: LatLngPoint) = RoadRouteResult(
        points = points.toList(),
        isRoadNetwork = true,
        providerName = "fake",
    )

    @Test
    fun fetchesDrivingRouteOnceAndCaches() = kotlinx.coroutines.runBlocking {
        val path = road(
            LatLngPoint(41.0, -100.0),
            LatLngPoint(41.0, -99.0),
            LatLngPoint(41.0, -98.0),
        )
        val fake = FakeDirections(mutableListOf(path))
        val session = RoadRouteSession(fake, offRouteHoldMs = 0)
        val dest = LatLngPoint(41.0, -98.0)
        val a = session.remainingRoad("me", LatLngPoint(41.0, -100.0), dest, nowMs = 1_000)
        val b = session.remainingRoad("me", LatLngPoint(41.0001, -99.5), dest, nowMs = 2_000)
        assertEquals(1, fake.fetchCount)
        assertTrue(a.size >= 2)
        assertTrue(b.size >= 2)
        assertEquals(dest.lng, a.last().lng, 1e-9)
        assertTrue(session.remainingRoadResult("me", LatLngPoint(41.0001, -99.5), dest, nowMs = 3_000).isRoadNetwork)
    }

    @Test
    fun reroutesAfterFiftyMetersAndTenSecondsOffRoute() = kotlinx.coroutines.runBlocking {
        val first = road(
            LatLngPoint(41.0, -100.0),
            LatLngPoint(41.0, -99.0),
            LatLngPoint(41.0, -98.0),
        )
        val second = road(
            LatLngPoint(41.01, -99.0),
            LatLngPoint(41.005, -98.5),
            LatLngPoint(41.0, -98.0),
        )
        val fake = FakeDirections(mutableListOf(first, second))
        val session = RoadRouteSession(
            fake,
            offRouteThresholdMeters = 50.0,
            offRouteHoldMs = 10_000,
        )
        val dest = LatLngPoint(41.0, -98.0)
        session.remainingRoad("me", LatLngPoint(41.0, -100.0), dest, nowMs = 0)
        // Still on route — no refetch
        session.remainingRoad("me", LatLngPoint(41.0, -99.2), dest, nowMs = 5_000)
        assertEquals(1, fake.fetchCount)
        // ~1.1 km north — start off-route clock
        session.remainingRoad("me", LatLngPoint(41.01, -99.0), dest, nowMs = 6_000)
        assertEquals(1, fake.fetchCount)
        // Hold not elapsed yet
        session.remainingRoad("me", LatLngPoint(41.01, -99.0), dest, nowMs = 12_000)
        assertEquals(1, fake.fetchCount)
        // 10s continuous off-route → refetch
        val rerouted = session.remainingRoad("me", LatLngPoint(41.01, -99.0), dest, nowMs = 16_500)
        assertEquals(2, fake.fetchCount)
        assertEquals(41.01, rerouted.first().lat, 1e-9)
        assertEquals(dest.lng, rerouted.last().lng, 1e-9)
    }

    @Test
    fun fallsBackToStraightWhenDirectionsDisabled() = kotlinx.coroutines.runBlocking {
        val fake = FakeDirections(mutableListOf(), configured = false)
        val session = RoadRouteSession(fake)
        val start = LatLngPoint(41.0, -100.0)
        val dest = LatLngPoint(41.0, -90.0)
        val result = session.remainingRoadResult("me", start, dest)
        assertEquals(2, result.points.size)
        assertFalse(result.isRoadNetwork)
        assertEquals(0, fake.fetchCount)
        assertEquals(start, result.points.first())
        assertEquals(dest, result.points.last())
    }
}

class FriendRoutePolylineBuilderRoadTest {
    @Test
    fun usesRoadRemainingWhenProvided() {
        val route = FriendActiveRoute(
            userId = "f1",
            displayName = "Ivan",
            loadRef = null,
            originLabel = "A",
            destinationLabel = "B",
            origin = LatLngPoint(47.6, -122.3),
            destination = LatLngPoint(45.5, -122.6),
            startDate = "2026-07-19",
            endDate = "2026-07-22",
            status = SharedLoadStatus.ACTIVE,
            trackPoints = emptyList(),
        )
        val road = listOf(
            LatLngPoint(46.5, -122.5),
            LatLngPoint(46.0, -122.55),
            LatLngPoint(45.5, -122.6),
        )
        val split = FriendRoutePolylineBuilder.split(
            route,
            current = LatLngPoint(46.5, -122.5),
            roadRemaining = road,
        )
        assertEquals(3, split.remaining.size)
        assertEquals(46.0, split.remaining[1].lat, 1e-9)
    }

    @Test
    fun usesRoadTraveledWhenProvided() {
        val route = FriendActiveRoute(
            userId = "f1",
            displayName = "Ivan",
            loadRef = null,
            originLabel = "A",
            destinationLabel = "B",
            origin = LatLngPoint(47.6, -122.3),
            destination = LatLngPoint(45.5, -122.6),
            startDate = "2026-07-19",
            endDate = "2026-07-22",
            status = SharedLoadStatus.ACTIVE,
            trackPoints = emptyList(),
        )
        val traveled = listOf(
            LatLngPoint(47.6, -122.3),
            LatLngPoint(47.0, -122.4),
            LatLngPoint(46.5, -122.5),
        )
        val split = FriendRoutePolylineBuilder.split(
            route,
            current = LatLngPoint(46.5, -122.5),
            roadTraveled = traveled,
        )
        assertEquals(3, split.past.size)
        assertEquals(47.0, split.past[1].lat, 1e-9)
    }
}
