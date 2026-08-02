package com.truckerload.data.remote

import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.DrivingRouteOptions
import com.truckerload.domain.friends.DrivingRouteResult
import com.truckerload.domain.friends.EncodedPolylineCodec
import com.truckerload.domain.friends.LatLngPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Public OSRM server — road polylines for the friends map.
 * Free, no API key. We intentionally do not use Google Directions API (paid).
 */
class OsrmDirectionsClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultClient,
) : DrivingDirectionsProvider {
    override val providerLabel: String = "OSRM"

    override fun isConfigured(): Boolean = baseUrl.isNotBlank()

    override suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        options: DrivingRouteOptions,
    ): Result<DrivingRouteResult> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("OSRM base URL missing"))
        }
        runCatching {
            val coords = "${origin.lng},${origin.lat};${destination.lng},${destination.lat}"
            val url = baseUrl.trimEnd('/').toHttpUrl().newBuilder()
                .addPathSegment("route")
                .addPathSegment("v1")
                .addPathSegment("driving")
                .addPathSegment(coords)
                .addQueryParameter("overview", "full")
                .addQueryParameter("geometries", "polyline")
                .addQueryParameter("steps", "false")
                .build()
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("OSRM HTTP ${resp.code}: $body")
                DrivingRouteResult(parseOsrmBody(body), providerLabel)
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
        val encoded = routes.getJSONObject(0).optString("geometry")
        val points = EncodedPolylineCodec.decode(encoded)
        if (points.size < 2) error("OSRM: polyline too short")
        return points
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://router.project-osrm.org"
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
