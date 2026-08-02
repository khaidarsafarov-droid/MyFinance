package com.truckerload.data.remote

import com.truckerload.BuildConfig
import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.EncodedPolylineCodec
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RouteFetchResult
import com.truckerload.domain.friends.VehicleRoutingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Google Directions API (driving) → decoded road polyline.
 *
 * Prefer [BuildConfig.GOOGLE_DIRECTIONS_API_KEY] (unrestricted / IP-restricted
 * browser/server key) over the Android-restricted Maps SDK key — package+SHA1
 * restrictions on [BuildConfig.GOOGLE_MAPS_API_KEY] often yield REQUEST_DENIED
 * for this HTTP endpoint even when map tiles load fine.
 *
 * Truck note: classic Directions has no vehicleHeight/weight/hazmat.
 * We use mode=driving and avoid=ferries for [VehicleRoutingProfile.TRUCK] as a
 * weak approximation. For production truck routing use TomTom Truck Routing API
 * or HERE Maps Truck (height, weight, axle load, tunnel category, hazmat).
 */
class GoogleDirectionsClient(
    private val apiKey: String = resolveApiKey(),
    private val client: OkHttpClient = defaultClient,
) : DrivingDirectionsProvider {
    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        profile: VehicleRoutingProfile,
    ): Result<RouteFetchResult> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("GOOGLE_MAPS/DIRECTIONS_API_KEY missing"))
        }
        runCatching {
            val urlBuilder = DIRECTIONS_BASE.toHttpUrl().newBuilder()
                .addQueryParameter("origin", "${origin.lat},${origin.lng}")
                .addQueryParameter("destination", "${destination.lat},${destination.lng}")
                .addQueryParameter("mode", "driving")
                .addQueryParameter("overview", "full")
                .addQueryParameter("key", apiKey)
            // Fallback truck prefs until a dedicated truck engine is wired.
            if (profile == VehicleRoutingProfile.TRUCK) {
                urlBuilder.addQueryParameter("avoid", "ferries")
            }
            val req = Request.Builder().url(urlBuilder.build()).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Directions HTTP ${resp.code}: $body")
                RouteFetchResult(points = parseDirectionsBody(body), source = SOURCE)
            }
        }.onFailure { e ->
            runCatching { android.util.Log.w(TAG, "Google Directions failed: ${e.message}") }
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
        const val SOURCE = "google"
        private const val TAG = "TL.Directions"
        private const val DIRECTIONS_BASE = "https://maps.googleapis.com/maps/api/directions/json"

        private fun resolveApiKey(): String {
            val directions = BuildConfig.GOOGLE_DIRECTIONS_API_KEY.trim()
            if (directions.isNotEmpty()) return directions
            return BuildConfig.GOOGLE_MAPS_API_KEY.trim()
        }

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
