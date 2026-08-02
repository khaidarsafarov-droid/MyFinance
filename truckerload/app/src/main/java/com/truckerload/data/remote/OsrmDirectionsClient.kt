package com.truckerload.data.remote

import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.EncodedPolylineCodec
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RouteFetchResult
import com.truckerload.domain.friends.VehicleRoutingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Keyless OSRM public demo → decoded road polyline.
 *
 * Used when Google Directions is missing, denied (Android-restricted Maps key),
 * or otherwise fails. The public demo only exposes a car `driving` profile —
 * there is no truck/height/weight model here.
 *
 * Production tip: self-host OSRM / use TomTom Truck or HERE Truck routing.
 */
class OsrmDirectionsClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultClient,
) : DrivingDirectionsProvider {
    override fun isConfigured(): Boolean = baseUrl.isNotBlank()

    override suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        profile: VehicleRoutingProfile,
    ): Result<RouteFetchResult> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("OSRM base URL missing"))
        }
        runCatching {
            // OSRM path is lon,lat;lon,lat (note order).
            val coords =
                "${origin.lng},${origin.lat};${destination.lng},${destination.lat}"
            // Public demo has no truck profile — always request driving roads.
            val url = "$baseUrl/route/v1/driving/$coords?overview=full&geometries=polyline"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("OSRM HTTP ${resp.code}: $body")
                RouteFetchResult(points = parseOsrmBody(body), source = SOURCE)
            }
        }
    }

    internal fun parseOsrmBody(body: String): List<LatLngPoint> {
        val json = JSONObject(body)
        val code = json.optString("code")
        if (!code.equals("Ok", ignoreCase = true)) {
            val msg = json.optString("message").ifBlank { code.ifBlank { "unknown" } }
            error("OSRM status $code: $msg")
        }
        val routes = json.optJSONArray("routes") ?: error("OSRM: no routes array")
        if (routes.length() == 0) error("OSRM: empty routes")
        val encoded = routes.getJSONObject(0).optString("geometry").orEmpty()
        val points = EncodedPolylineCodec.decode(encoded)
        if (points.size < 2) error("OSRM: polyline too short")
        return points
    }

    companion object {
        const val SOURCE = "osrm"
        /** Project OSRM public demo — rate-limited; fine for fallback / offline-dev. */
        const val DEFAULT_BASE_URL = "https://router.project-osrm.org"
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
