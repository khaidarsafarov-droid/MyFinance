package com.truckerload.data.remote

import com.truckerload.BuildConfig
import com.truckerload.domain.friends.DrivingDirectionsProvider
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
 * Google Directions API (driving) → decoded road polyline.
 * Uses the same Maps key as the SDK ([BuildConfig.GOOGLE_MAPS_API_KEY]);
 * Directions API must be enabled on that Cloud project.
 */
class GoogleDirectionsClient(
    private val apiKey: String = BuildConfig.GOOGLE_MAPS_API_KEY,
    private val client: OkHttpClient = defaultClient,
) : DrivingDirectionsProvider {
    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
    ): Result<List<LatLngPoint>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("GOOGLE_MAPS_API_KEY missing"))
        }
        runCatching {
            val url = DIRECTIONS_BASE.toHttpUrl().newBuilder()
                .addQueryParameter("origin", "${origin.lat},${origin.lng}")
                .addQueryParameter("destination", "${destination.lat},${destination.lng}")
                .addQueryParameter("mode", "driving")
                .addQueryParameter("overview", "full")
                .addQueryParameter("key", apiKey)
                .build()
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Directions HTTP ${resp.code}: $body")
                parseDirectionsBody(body)
            }
        }
    }

    internal fun parseDirectionsBody(body: String): List<LatLngPoint> {
        val json = JSONObject(body)
        val status = json.optString("status")
        if (status != "OK") {
            val msg = json.optString("error_message").ifBlank { status.ifBlank { "unknown" } }
            error("Directions status $status: $msg")
        }
        val routes = json.optJSONArray("routes") ?: error("Directions: no routes array")
        if (routes.length() == 0) error("Directions: empty routes")
        val encoded = routes.getJSONObject(0)
            .optJSONObject("overview_polyline")
            ?.optString("points")
            .orEmpty()
        val points = EncodedPolylineCodec.decode(encoded)
        if (points.size < 2) error("Directions: polyline too short")
        return points
    }

    companion object {
        private const val DIRECTIONS_BASE = "https://maps.googleapis.com/maps/api/directions/json"
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
