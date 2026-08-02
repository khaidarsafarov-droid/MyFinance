package com.truckerload.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDirectionsClientTest {

    private val client = GoogleDirectionsClient(apiKey = "test-key")

    @Test
    fun parseOkResponseDecodesOverviewPolyline() {
        // Same encoded sample as EncodedPolylineCodecTest
        val body = """
            {
              "status": "OK",
              "routes": [{
                "overview_polyline": {
                  "points": "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
                }
              }]
            }
        """.trimIndent()
        val points = client.parseDirectionsBody(body)
        assertEquals(3, points.size)
        assertEquals(38.5, points.first().lat, 0.01)
        assertEquals(-126.453, points.last().lng, 0.01)
    }

    @Test
    fun parseNonOkThrows() {
        val body = """{"status":"REQUEST_DENIED","error_message":"API not enabled"}"""
        val error = runCatching { client.parseDirectionsBody(body) }.exceptionOrNull()
        assertTrue(error is IllegalStateException || error is Exception)
        assertTrue(error!!.message!!.contains("REQUEST_DENIED"))
    }
}

class OsrmDirectionsClientTest {

    private val client = OsrmDirectionsClient()

    @Test
    fun parseOkResponseDecodesGeometryPolyline() {
        val body = """
            {
              "code": "Ok",
              "routes": [{
                "geometry": "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
              }]
            }
        """.trimIndent()
        val points = client.parseOsrmBody(body)
        assertEquals(3, points.size)
        assertEquals(38.5, points.first().lat, 0.01)
        assertEquals(-126.453, points.last().lng, 0.01)
    }

    @Test
    fun parseNonOkThrows() {
        val body = """{"code":"NoRoute","message":"No route found"}"""
        val error = runCatching { client.parseOsrmBody(body) }.exceptionOrNull()
        assertTrue(error!!.message!!.contains("NoRoute"))
    }
}

class CompositeDrivingDirectionsProviderTest {

    private class FakeProvider(
        private val configured: Boolean,
        private val result: Result<com.truckerload.domain.friends.RouteFetchResult>,
    ) : com.truckerload.domain.friends.DrivingDirectionsProvider {
        var fetchCount = 0
        override fun isConfigured(): Boolean = configured
        override suspend fun fetchDrivingRoute(
            origin: com.truckerload.domain.friends.LatLngPoint,
            destination: com.truckerload.domain.friends.LatLngPoint,
            profile: com.truckerload.domain.friends.VehicleRoutingProfile,
        ): Result<com.truckerload.domain.friends.RouteFetchResult> {
            fetchCount++
            return result
        }
    }

    @Test
    fun usesPrimaryWhenSuccessful() = kotlinx.coroutines.runBlocking {
        val road = listOf(
            com.truckerload.domain.friends.LatLngPoint(41.0, -100.0),
            com.truckerload.domain.friends.LatLngPoint(41.0, -98.0),
        )
        val primary = FakeProvider(
            configured = true,
            result = Result.success(
                com.truckerload.domain.friends.RouteFetchResult(road, "google"),
            ),
        )
        val fallback = FakeProvider(
            configured = true,
            result = Result.success(
                com.truckerload.domain.friends.RouteFetchResult(road, "osrm"),
            ),
        )
        val composite = CompositeDrivingDirectionsProvider(primary, fallback)
        val out = composite.fetchDrivingRoute(
            com.truckerload.domain.friends.LatLngPoint(41.0, -100.0),
            com.truckerload.domain.friends.LatLngPoint(41.0, -98.0),
        ).getOrThrow()
        assertEquals("google", out.source)
        assertEquals(1, primary.fetchCount)
        assertEquals(0, fallback.fetchCount)
    }

    @Test
    fun fallsBackWhenPrimaryFails() = kotlinx.coroutines.runBlocking {
        val road = listOf(
            com.truckerload.domain.friends.LatLngPoint(41.0, -100.0),
            com.truckerload.domain.friends.LatLngPoint(41.0, -98.0),
        )
        val primary = FakeProvider(
            configured = true,
            result = Result.failure(IllegalStateException("REQUEST_DENIED")),
        )
        val fallback = FakeProvider(
            configured = true,
            result = Result.success(
                com.truckerload.domain.friends.RouteFetchResult(road, "osrm"),
            ),
        )
        val composite = CompositeDrivingDirectionsProvider(primary, fallback)
        val out = composite.fetchDrivingRoute(
            com.truckerload.domain.friends.LatLngPoint(41.0, -100.0),
            com.truckerload.domain.friends.LatLngPoint(41.0, -98.0),
        ).getOrThrow()
        assertEquals("osrm", out.source)
        assertEquals(1, primary.fetchCount)
        assertEquals(1, fallback.fetchCount)
    }

    @Test
    fun usesFallbackWhenPrimaryNotConfigured() = kotlinx.coroutines.runBlocking {
        val road = listOf(
            com.truckerload.domain.friends.LatLngPoint(41.0, -100.0),
            com.truckerload.domain.friends.LatLngPoint(41.0, -98.0),
        )
        val primary = FakeProvider(
            configured = false,
            result = Result.failure(IllegalStateException("missing key")),
        )
        val fallback = FakeProvider(
            configured = true,
            result = Result.success(
                com.truckerload.domain.friends.RouteFetchResult(road, "osrm"),
            ),
        )
        val composite = CompositeDrivingDirectionsProvider(primary, fallback)
        val out = composite.fetchDrivingRoute(
            com.truckerload.domain.friends.LatLngPoint(41.0, -100.0),
            com.truckerload.domain.friends.LatLngPoint(41.0, -98.0),
        ).getOrThrow()
        assertEquals("osrm", out.source)
        assertEquals(0, primary.fetchCount)
        assertEquals(1, fallback.fetchCount)
    }
}
