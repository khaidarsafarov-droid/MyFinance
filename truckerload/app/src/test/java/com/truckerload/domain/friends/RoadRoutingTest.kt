package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineDecoderTest {

    @Test
    fun decodesGoogleSamplePolyline() {
        // Google's canonical example: (38.5, -120.2) → (40.7, -120.95) → (43.252, -126.453)
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = PolylineDecoder.decode(encoded)
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].lat, 0.001)
        assertEquals(-120.2, points[0].lng, 0.001)
        assertEquals(40.7, points[1].lat, 0.001)
        assertEquals(-120.95, points[1].lng, 0.001)
        assertEquals(43.252, points[2].lat, 0.001)
        assertEquals(-126.453, points[2].lng, 0.001)
    }

    @Test
    fun emptyStringReturnsEmptyList() {
        assertTrue(PolylineDecoder.decode("").isEmpty())
    }
}

class RouteDeviationDetectorTest {

    @Test
    fun pointOnSegmentIsNotDeviated() {
        val route = listOf(
            LatLngPoint(40.0, -100.0),
            LatLngPoint(41.0, -100.0),
        )
        val onRoute = LatLngPoint(40.5, -100.0)
        assertFalse(RouteDeviationDetector.isDeviated(onRoute, route))
    }

    @Test
    fun pointFarFromSegmentIsDeviated() {
        val route = listOf(
            LatLngPoint(40.0, -100.0),
            LatLngPoint(41.0, -100.0),
        )
        val offRoute = LatLngPoint(40.5, -95.0)
        assertTrue(RouteDeviationDetector.isDeviated(offRoute, route))
    }

    @Test
    fun shortPolylineReturnsInfiniteDistance() {
        val dist = RouteDeviationDetector.distanceToPolylineKm(
            LatLngPoint(40.0, -100.0),
            listOf(LatLngPoint(41.0, -100.0)),
        )
        assertTrue(dist.isInfinite())
    }
}
