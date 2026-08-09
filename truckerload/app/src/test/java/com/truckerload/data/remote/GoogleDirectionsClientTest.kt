package com.truckerload.data.remote

import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RouteRequest
import com.truckerload.domain.friends.VehicleRoutingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDirectionsClientTest {

    private val client = GoogleDirectionsClient(apiKey = "test-key")

    @Test
    fun parseOkResponseDecodesOverviewPolyline() {
        val body = """
            {
              "status": "OK",
              "routes": [{
                "overview_polyline": {
                  "points": "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
                },
                "legs": [{
                  "distance": {"value": 160934},
                  "duration": {"value": 7200}
                }]
              }]
            }
        """.trimIndent()
        val result = client.parseDirectionsBody(body)
        assertTrue(result.isRoadNetwork)
        assertEquals(3, result.points.size)
        assertEquals(38.5, result.points.first().lat, 0.01)
        assertEquals(-126.453, result.points.last().lng, 0.01)
        assertEquals(160934L, result.distanceMeters)
        assertEquals(7200L, result.durationSeconds)
        assertEquals(GoogleDirectionsClient.PROVIDER_NAME, result.providerName)
    }

    @Test
    fun parseNonOkThrows() {
        val body = """{"status":"REQUEST_DENIED","error_message":"API not enabled"}"""
        val error = runCatching { client.parseDirectionsBody(body) }.exceptionOrNull()
        assertTrue(error is IllegalStateException || error is Exception)
        assertTrue(error!!.message!!.contains("REQUEST_DENIED"))
    }

    @Test
    fun truckQueryUsesDrivingModeWithAvoidances() {
        val query = client.buildQuery(
            RouteRequest(
                origin = LatLngPoint(41.0, -100.0),
                destination = LatLngPoint(41.0, -90.0),
                vehicleMode = VehicleRoutingMode.TRUCK,
            ),
        )
        assertTrue(query.contains("mode=driving"))
        assertTrue(query.contains("avoid=tolls") || query.contains("avoid=tolls%7Cferries") || query.contains("avoid=tolls|ferries"))
        assertFalse(query.contains("mode=truck"))
    }

    @Test
    fun carQueryOmitsTruckAvoidances() {
        val query = client.buildQuery(
            RouteRequest(
                origin = LatLngPoint(41.0, -100.0),
                destination = LatLngPoint(41.0, -90.0),
                vehicleMode = VehicleRoutingMode.CAR,
            ),
        )
        assertTrue(query.contains("mode=driving"))
        assertFalse(query.contains("avoid="))
    }

    @Test
    fun resolveApiKeyPrefersDirectionsKey() {
        assertEquals(
            "dir-key",
            GoogleDirectionsClient.resolveApiKey(directionsKey = "dir-key", mapsKey = "maps-key"),
        )
        assertEquals(
            "maps-key",
            GoogleDirectionsClient.resolveApiKey(directionsKey = "  ", mapsKey = "maps-key"),
        )
    }
}

class OsrmDirectionsClientTest {

    private val client = OsrmDirectionsClient()

    @Test
    fun parseOkResponseDecodesGeometry() {
        val body = """
            {
              "code": "Ok",
              "routes": [{
                "geometry": "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                "distance": 80467.2,
                "duration": 3600.5
              }]
            }
        """.trimIndent()
        val result = client.parseOsrmBody(body)
        assertTrue(result.isRoadNetwork)
        assertEquals(3, result.points.size)
        assertEquals(80467L, result.distanceMeters)
        assertEquals(3600L, result.durationSeconds)
        assertEquals(OsrmDirectionsClient.PROVIDER_NAME, result.providerName)
    }
}

class CompositeDirectionsProviderTest {

    @Test
    fun fallsThroughToSecondProvider() = kotlinx.coroutines.runBlocking {
        val failing = object : com.truckerload.domain.friends.DrivingDirectionsProvider {
            override fun isConfigured() = true
            override suspend fun fetchRoute(request: RouteRequest): Result<com.truckerload.domain.friends.RoadRouteResult> =
                Result.failure(IllegalStateException("REQUEST_DENIED"))
        }
        val osrmLike = object : com.truckerload.domain.friends.DrivingDirectionsProvider {
            override fun isConfigured() = true
            override suspend fun fetchRoute(request: RouteRequest): Result<com.truckerload.domain.friends.RoadRouteResult> =
                Result.success(
                    com.truckerload.domain.friends.RoadRouteResult(
                        points = listOf(request.origin, LatLngPoint(41.0, -95.0), request.destination),
                        isRoadNetwork = true,
                        providerName = "osrm",
                    ),
                )
        }
        val composite = CompositeDirectionsProvider(listOf(failing, osrmLike))
        val route = composite.fetchRoute(
            RouteRequest(LatLngPoint(41.0, -100.0), LatLngPoint(41.0, -90.0)),
        ).getOrThrow()
        assertEquals("osrm", route.providerName)
        assertEquals(3, route.points.size)
    }
}
