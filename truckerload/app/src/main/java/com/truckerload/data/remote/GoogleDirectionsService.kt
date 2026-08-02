package com.truckerload.data.remote

import com.truckerload.BuildConfig
import com.truckerload.domain.friends.DirectionsProvider
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.PolylineDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches driving directions from the Google Directions API and returns road-following polylines.
 *
 * Requires [BuildConfig.GOOGLE_MAPS_API_KEY] and the Directions API enabled on the same
 * Google Cloud project as the Maps SDK key.
 */
@Singleton
class GoogleDirectionsService @Inject constructor() : DirectionsProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank()

    /**
     * Returns road geometry from [origin] to [destination], or null when the API is
     * unavailable or returns no route.
     */
    override
    suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
    ): List<LatLngPoint>? = withContext(Dispatchers.IO) {
        val key = BuildConfig.GOOGLE_MAPS_API_KEY.trim()
        if (key.isBlank()) return@withContext null
        val url = buildString {
            append("https://maps.googleapis.com/maps/api/directions/json")
            append("?origin=").append(origin.lat).append(',').append(origin.lng)
            append("&destination=").append(destination.lat).append(',').append(destination.lng)
            append("&mode=driving")
            append("&key=").append(key)
        }
        runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body?.string().orEmpty()
                parseDirectionsResponse(body)
            }
        }.getOrNull()
    }

    internal fun parseDirectionsResponse(json: String): List<LatLngPoint>? {
        if (json.isBlank()) return null
        val root = JSONObject(json)
        if (root.optString("status") != "OK") return null
        val routes = root.optJSONArray("routes") ?: return null
        if (routes.length() == 0) return null
        val encoded = routes.getJSONObject(0)
            .optJSONObject("overview_polyline")
            ?.optString("points")
            .orEmpty()
        if (encoded.isBlank()) return null
        val decoded = PolylineDecoder.decode(encoded)
        return decoded.takeIf { it.size >= 2 }
    }
}
