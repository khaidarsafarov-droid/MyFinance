package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncodedPolylineCodecTest {

    @Test
    fun decodesGoogleSamplePolyline() {
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
    fun onRouteWhenNearPolyline() {
        val me = LatLngPoint(41.0001, -98.5)
        assertFalse(RoadRouteGeometry.isOffRoute(me, i80Corridor, thresholdMeters = 50.0))
    }

    @Test
    fun offRouteWhenFarFromPolyline() {
        val me = LatLngPoint(42.5, -98.5)
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
}

class RoadRouteSessionTest {

    private class FakeDirections(
        private var responses: MutableList<List<LatLngPoint>>,
        private val configured: Boolean = true,
    ) : DrivingDirectionsProvider {
        override val providerLabel: String = "Fake"
        var fetchCount = 0
        override fun isConfigured(): Boolean = configured
        override suspend fun fetchDrivingRoute(
            origin: LatLngPoint,
            destination: LatLngPoint,
            options: DrivingRouteOptions,
        ): Result<DrivingRouteResult> {
            fetchCount++
            val next = responses.removeFirstOrNull()
                ?: return Result.failure(IllegalStateException("no more fakes"))
            return Result.success(DrivingRouteResult(next, providerLabel))
        }
    }

    @Test
    fun fetchesDrivingRouteOnceAndCaches() = kotlinx.coroutines.runBlocking {
        val road = listOf(
            LatLngPoint(41.0, -100.0),
            LatLngPoint(41.0, -99.0),
            LatLngPoint(41.0, -98.0),
        )
        val fake = FakeDirections(mutableListOf(road))
        val session = RoadRouteSession(fake, minRerouteIntervalMs = 0)
        val dest = LatLngPoint(41.0, -98.0)
        val a = session.remainingRoad("me", LatLngPoint(41.0, -100.0), dest, nowMs = 1_000)
        val b = session.remainingRoad("me", LatLngPoint(41.001, -99.5), dest, nowMs = 2_000)
        assertEquals(1, fake.fetchCount)
        assertTrue(a.points.size >= 2)
        assertTrue(b.points.size >= 2)
        assertTrue(a.isRoadRouted)
        assertEquals(dest.lng, a.points.last().lng, 1e-9)
    }

    @Test
    fun reroutesWhenSustainedOffCorridorAfterThrottle() = kotlinx.coroutines.runBlocking {
        val first = listOf(
            LatLngPoint(41.0, -100.0),
            LatLngPoint(41.0, -99.0),
            LatLngPoint(41.0, -98.0),
        )
        val second = listOf(
            LatLngPoint(42.0, -99.0),
            LatLngPoint(41.5, -98.5),
            LatLngPoint(41.0, -98.0),
        )
        val fake = FakeDirections(mutableListOf(first, second))
        val session = RoadRouteSession(
            fake,
            offRouteThresholdMeters = 800.0,
            offRouteDurationMs = 10_000L,
            minRerouteIntervalMs = 1_000,
        )
        val dest = LatLngPoint(41.0, -98.0)
        session.remainingRoad("me", LatLngPoint(41.0, -100.0), dest, nowMs = 0)
        session.remainingRoad("me", LatLngPoint(41.0, -99.2), dest, nowMs = 500)
        assertEquals(1, fake.fetchCount)
        // Off corridor but not sustained long enough
        session.remainingRoad("me", LatLngPoint(42.0, -99.0), dest, nowMs = 5_000)
        assertEquals(1, fake.fetchCount)
        // Sustained off-route + throttle elapsed
        val rerouted = session.remainingRoad("me", LatLngPoint(42.0, -99.0), dest, nowMs = 15_000)
        assertEquals(2, fake.fetchCount)
        assertEquals(42.0, rerouted.points.first().lat, 1e-9)
        assertEquals(dest.lng, rerouted.points.last().lng, 1e-9)
    }

    @Test
    fun fallsBackToStraightWhenDirectionsDisabled() = kotlinx.coroutines.runBlocking {
        val fake = FakeDirections(mutableListOf(), configured = false)
        val session = RoadRouteSession(fake)
        val start = LatLngPoint(41.0, -100.0)
        val dest = LatLngPoint(41.0, -90.0)
        val path = session.remainingRoad("me", start, dest)
        assertEquals(2, path.points.size)
        assertEquals(0, fake.fetchCount)
        assertFalse(path.isRoadRouted)
        assertEquals(start, path.points.first())
        assertEquals(dest, path.points.last())
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
}
