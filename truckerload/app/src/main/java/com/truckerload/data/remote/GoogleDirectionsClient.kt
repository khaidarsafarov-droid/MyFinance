package com.truckerload.data.remote

import com.truckerload.BuildConfig
import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.EncodedPolylineCodec
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RoadRouteResult
import com.truckerload.domain.friends.RouteRequest
import com.truckerload.domain.friends.VehicleRoutingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Google Directions API → decoded road polyline.
 *
 * Uses [BuildConfig.GOOGLE_DIRECTIONS_API_KEY] when set, otherwise the Maps SDK key.
 * Android-package–restricted Maps keys often work for tiles but return REQUEST_DENIED
 * for HTTPS Directions — prefer a separate unrestricted / IP-restricted Directions key.
 *
 * Classic Directions has no `mode=truck`. For [VehicleRoutingMode.TRUCK] we request
 * `mode=driving` with `avoid=tolls|ferries` and keep vehicle dimensions on [RouteRequest]
 * for a future TomTom Truck Routing / HERE truck backend. Production truck constraints
 * (bridge weight, hazmat tunnels, height) need that specialized API.
 */
class GoogleDirectionsClient(
    private val apiKey: String = resolveApiKey(),
    private val client: OkHttpClient = defaultClient,
) : DrivingDirectionsProvider {
    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun fetchRoute(request: RouteRequest): Result<RoadRouteResult> =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) {
                return@withContext Result.failure(IllegalStateException("GOOGLE_MAPS_API_KEY / GOOGLE_DIRECTIONS_API_KEY missing"))
            }
            runCatching {
                val urlBuilder = DIRECTIONS_BASE.toHttpUrl().newBuilder()
                    .addQueryParameter("origin", "${request.origin.lat},${request.origin.lng}")
                    .addQueryParameter("destination", "${request.destination.lat},${request.destination.lng}")
                    // Google Directions has no truck travel mode — see class KDoc.
                    .addQueryParameter("mode", "driving")
                    .addQueryParameter("overview", "full")
                    .addQueryParameter("key", apiKey)
                if (request.vehicleMode == VehicleRoutingMode.TRUCK) {
                    // Best-effort truck bias until TomTom/HERE truck routing is wired.
                    urlBuilder.addQueryParameter("avoid", "tolls|ferries")
                }
                val req = Request.Builder().url(urlBuilder.build()).get().build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("Directions HTTP ${resp.code}: $body")
                    parseDirectionsBody(body)
                }
            }
        }

    internal fun parseDirectionsBody(body: String): RoadRouteResult {
        val json = JSONObject(body)
        val status = json.optString("status")
        if (status != "OK") {
            val msg = json.optString("error_message").ifBlank { status.ifBlank { "unknown" } }
            error("Directions status $status: $msg")
        }
        val routes = json.optJSONArray("routes") ?: error("Directions: no routes array")
        if (routes.length() == 0) error("Directions: empty routes")
        val route = routes.getJSONObject(0)
        val encoded = route
            .optJSONObject("overview_polyline")
            ?.optString("points")
            .orEmpty()
        val points = EncodedPolylineCodec.decode(encoded)
        if (points.size < 2) error("Directions: polyline too short")
        var distance = 0L
        var duration = 0L
        val legs = route.optJSONArray("legs")
        if (legs != null) {
            for (i in 0 until legs.length()) {
                val leg = legs.getJSONObject(i)
                distance += leg.optJSONObject("distance")?.optLong("value") ?: 0L
                duration += leg.optJSONObject("duration")?.optLong("value") ?: 0L
            }
        }
        return RoadRouteResult(
            points = points,
            isRoadNetwork = true,
            distanceMeters = distance.takeIf { it > 0L },
            durationSeconds = duration.takeIf { it > 0L },
            providerName = PROVIDER_NAME,
        )
    }

    /** Builds the query string used for HTTP so unit tests can assert truck avoidances. */
    internal fun buildQuery(request: RouteRequest): String {
        val urlBuilder = DIRECTIONS_BASE.toHttpUrl().newBuilder()
            .addQueryParameter("origin", "${request.origin.lat},${request.origin.lng}")
            .addQueryParameter("destination", "${request.destination.lat},${request.destination.lng}")
            .addQueryParameter("mode", "driving")
            .addQueryParameter("overview", "full")
            .addQueryParameter("key", apiKey.ifBlank { "test" })
        if (request.vehicleMode == VehicleRoutingMode.TRUCK) {
            urlBuilder.addQueryParameter("avoid", "tolls|ferries")
        }
        return urlBuilder.build().query.orEmpty()
    }

    companion object {
        const val PROVIDER_NAME = "google_directions"
        private const val DIRECTIONS_BASE = "https://maps.googleapis.com/maps/api/directions/json"
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        fun resolveApiKey(
            directionsKey: String = BuildConfig.GOOGLE_DIRECTIONS_API_KEY,
            mapsKey: String = BuildConfig.GOOGLE_MAPS_API_KEY,
        ): String = directionsKey.trim().ifBlank { mapsKey.trim() }
    }
}
