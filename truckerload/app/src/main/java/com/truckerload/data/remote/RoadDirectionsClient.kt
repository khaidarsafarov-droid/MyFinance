package com.truckerload.data.remote

import android.util.Log
import com.truckerload.BuildConfig
import com.truckerload.domain.friends.EncodedPolylineCodec
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RoadRouteGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Fetches driving geometry that follows roads.
 *
 * 1. Google Directions API (when [BuildConfig.GOOGLE_MAPS_API_KEY] is set and Directions is enabled)
 * 2. Public OSRM fallback so the map still shows a road path without extra Cloud setup
 *
 * Results are cached by rounded origin/destination buckets; callers should replan when
 * [RoadRouteGeometry.isOffRoute] or the destination changes.
 */
class RoadDirectionsClient(
    private val apiKey: String = BuildConfig.GOOGLE_MAPS_API_KEY,
    private val client: OkHttpClient = defaultClient(),
) {

    private val cache = ConcurrentHashMap<String, List<LatLngPoint>>()

    suspend fun routeAlongRoads(
        origin: LatLngPoint,
        destination: LatLngPoint,
        forceRefresh: Boolean = false,
    ): List<LatLngPoint> = withContext(Dispatchers.IO) {
        if (samePoint(origin, destination)) return@withContext listOf(origin, destination)
        val key = RoadRouteGeometry.routeCacheKey(origin, destination)
        if (!forceRefresh) {
            cache[key]?.takeIf { it.size >= 2 }?.let { return@withContext it }
        }
        val path = fetchGoogleDirections(origin, destination)
            ?: fetchOsrm(origin, destination)
            ?: listOf(origin, destination)
        if (path.size >= 2) cache[key] = path
        path
    }

    fun clearCache() {
        cache.clear()
    }

    private fun fetchGoogleDirections(
        origin: LatLngPoint,
        destination: LatLngPoint,
    ): List<LatLngPoint>? {
        val key = apiKey.trim()
        if (key.isBlank()) return null
        val url =
            "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=${origin.lat},${origin.lng}" +
                "&destination=${destination.lat},${destination.lng}" +
                "&mode=driving&overview=full&key=$key"
        return runCatching {
            val body = httpGet(url) ?: return null
            val json = JSONObject(body)
            val status = json.optString("status")
            if (status != "OK") {
                Log.w(TAG, "Google Directions status=$status")
                return null
            }
            val routes = json.optJSONArray("routes") ?: return null
            if (routes.length() == 0) return null
            val points = routes.getJSONObject(0)
                .optJSONObject("overview_polyline")
                ?.optString("points")
                .orEmpty()
            EncodedPolylineCodec.decode(points).takeIf { it.size >= 2 }
        }.onFailure { e ->
            Log.w(TAG, "Google Directions failed", e)
        }.getOrNull()
    }

    private fun fetchOsrm(
        origin: LatLngPoint,
        destination: LatLngPoint,
    ): List<LatLngPoint>? {
        // OSRM expects lon,lat
        val url =
            "https://router.project-osrm.org/route/v1/driving/" +
                "${origin.lng},${origin.lat};${destination.lng},${destination.lat}" +
                "?overview=full&geometries=polyline"
        return runCatching {
            val body = httpGet(url) ?: return null
            val json = JSONObject(body)
            if (json.optString("code") != "Ok") {
                Log.w(TAG, "OSRM code=${json.optString("code")}")
                return null
            }
            val routes = json.optJSONArray("routes") ?: return null
            if (routes.length() == 0) return null
            val geometry = routes.getJSONObject(0).optString("geometry")
            EncodedPolylineCodec.decode(geometry).takeIf { it.size >= 2 }
        }.onFailure { e ->
            Log.w(TAG, "OSRM failed", e)
        }.getOrNull()
    }

    private fun httpGet(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP ${response.code} for directions")
                return null
            }
            return response.body?.string()
        }
    }

    private fun samePoint(a: LatLngPoint, b: LatLngPoint): Boolean =
        kotlin.math.abs(a.lat - b.lat) < 1e-5 && kotlin.math.abs(a.lng - b.lng) < 1e-5

    companion object {
        private const val TAG = "RoadDirections"

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
    }
}
