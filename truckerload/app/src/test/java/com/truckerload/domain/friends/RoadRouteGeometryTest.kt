package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncodedPolylineCodecTest {

    @Test
    fun decodesGoogleSamplePolyline() {
        // Encoded form of a short segment near (38.5, -120.2) → (40.7, -120.95) → (43.252, -126.453)
        val points = EncodedPolylineCodec.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].lat, 0.01)
        assertEquals(-120.2, points[0].lng, 0.01)
        assertEquals(40.7, points[1].lat, 0.01)
        assertEquals(-120.95, points[1].lng, 0.01)
    }

    @Test
    fun blankReturnsEmpty() {
        assertTrue(EncodedPolylineCodec.decode("").isEmpty())
    }
}

class RoadRouteGeometryTest {

    private val path = listOf(
        LatLngPoint(41.0, -96.0),
        LatLngPoint(41.0, -95.0),
        LatLngPoint(41.0, -94.0),
        LatLngPoint(41.0, -93.0),
        LatLngPoint(41.0, -90.0),
    )

    @Test
    fun remainingStartsAtCurrentAndFollowsRoadEast() {
        val current = LatLngPoint(41.0, -94.5)
        val remaining = RoadRouteGeometry.remainingFromNearest(path, current)
        assertTrue(remaining.size >= 3)
        assertEquals(41.0, remaining.first().lat, 0.001)
        assertEquals(-94.5, remaining.first().lng, 0.001)
        assertEquals(-90.0, remaining.last().lng, 0.001)
    }

    @Test
    fun offRouteWhenFarFromCorridor() {
        assertFalse(RoadRouteGeometry.isOffRoute(path, LatLngPoint(41.0, -94.0), 800.0))
        assertTrue(RoadRouteGeometry.isOffRoute(path, LatLngPoint(42.5, -94.0), 800.0))
    }

    @Test
    fun cacheBucketsNearbyGpsJitterTogether() {
        val a = LatLngPoint(41.1234, -96.1234)
        val b = LatLngPoint(41.1239, -96.1231)
        assertEquals(RoadRouteGeometry.cacheBucket(a), RoadRouteGeometry.cacheBucket(b))
    }
}

class FriendRoutePolylineBuilderRoadTest {

    @Test
    fun usesRoadPathForRemainingInsteadOfStraightChord() {
        val route = FriendActiveRoute(
            userId = "f1",
            displayName = "Ivan",
            loadRef = null,
            originLabel = "A",
            destinationLabel = "B",
            origin = LatLngPoint(41.0, -96.0),
            destination = LatLngPoint(41.0, -90.0),
            startDate = "2026-07-19",
            endDate = "2026-07-22",
            status = SharedLoadStatus.ACTIVE,
            trackPoints = emptyList(),
        )
        // Road dips south then east — not a straight line.
        val road = listOf(
            LatLngPoint(41.0, -96.0),
            LatLngPoint(40.0, -94.0),
            LatLngPoint(41.0, -90.0),
        )
        val split = FriendRoutePolylineBuilder.split(
            route,
            current = LatLngPoint(41.0, -96.0),
            roadPath = road,
        )
        assertTrue(split.remaining.size >= 3)
        assertTrue(split.remaining.any { it.lat < 40.5 })
        assertEquals(41.0, split.remaining.last().lat, 0.001)
        assertEquals(-90.0, split.remaining.last().lng, 0.001)
    }
}
