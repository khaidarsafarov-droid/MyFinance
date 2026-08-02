package com.truckerload.data.remote

import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.EncodedPolylineCodec
import com.truckerload.domain.friends.RoadRouteResult
import com.truckerload.domain.friends.RouteRequest
import com.truckerload.domain.friends.VehicleRoutingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OSRM road-network fallback when Google Directions is unavailable
 * (missing key, Android-restricted key → REQUEST_DENIED, offline billing, etc.).
 *
 * Uses the public OSRM demo server with `profile=driving`. There is no true
 * truck profile here — for production truck constraints (height / weight /
 * hazmat) wire TomTom Truck Routing or HERE Maps; keep [RouteRequest.truck]
 * populated for that migration.
 */
class OsrmDirectionsClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultClient,
    private val enabled: Boolean = true,
) : DrivingDirectionsProvider {
    override fun isConfigured(): Boolean = enabled && baseUrl.isNotBlank()

    override suspend fun fetchRoute(request: RouteRequest): Result<RoadRouteResult> =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) {
                return@withContext Result.failure(IllegalStateException("OSRM disabled"))
            }
            runCatching {
                // OSRM expects lon,lat; public demo only exposes car driving.
                // Truck params are intentionally unused until a truck-capable backend
                // (TomTom / HERE) replaces this fallback — see class KDoc.
                @Suppress("UNUSED_VARIABLE")
                val truckHint = request.vehicleMode == VehicleRoutingMode.TRUCK
                val coords = listOf(
                    "${request.origin.lng},${request.origin.lat}",
                    "${request.destination.lng},${request.destination.lat}",
                ).joinToString(";")
                val url = "$baseUrl/route/v1/driving/$coords?overview=full&geometries=polyline"
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("OSRM HTTP ${resp.code}: $body")
                    parseOsrmBody(body)
                }
            }
        }

    internal fun parseOsrmBody(body: String): RoadRouteResult {
        val json = JSONObject(body)
        val code = json.optString("code")
        if (code != "Ok") {
            error("OSRM status $code: ${json.optString("message").ifBlank { code }}")
        }
        val routes = json.optJSONArray("routes") ?: error("OSRM: no routes")
        if (routes.length() == 0) error("OSRM: empty routes")
        val route = routes.getJSONObject(0)
        val encoded = route.optString("geometry")
        val points = EncodedPolylineCodec.decode(encoded)
        if (points.size < 2) error("OSRM: polyline too short")
        val distance = route.optDouble("distance", 0.0).toLong()
        val duration = route.optDouble("duration", 0.0).toLong()
        return RoadRouteResult(
            points = points,
            isRoadNetwork = true,
            distanceMeters = distance.takeIf { it > 0L },
            durationSeconds = duration.takeIf { it > 0L },
            providerName = PROVIDER_NAME,
        )
    }

    companion object {
        const val PROVIDER_NAME = "osrm"
        const val DEFAULT_BASE_URL = "https://router.project-osrm.org"
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
