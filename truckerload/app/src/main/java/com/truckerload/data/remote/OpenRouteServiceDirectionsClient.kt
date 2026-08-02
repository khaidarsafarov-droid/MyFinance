package com.truckerload.data.remote

import com.truckerload.BuildConfig
import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.DrivingRouteOptions
import com.truckerload.domain.friends.DrivingRouteResult
import com.truckerload.domain.friends.EncodedPolylineCodec
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.VehicleRoutingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenRouteService directions — supports HGV / truck restrictions when
 * [OPENROUTESERVICE_API_KEY] is set in local.properties.
 *
 * Free tier: https://openrouteservice.org/dev/#/signup
 */
class OpenRouteServiceDirectionsClient(
    private val apiKey: String = BuildConfig.OPENROUTESERVICE_API_KEY,
    private val client: OkHttpClient = defaultClient,
) : DrivingDirectionsProvider {
    override val providerLabel: String = "OpenRouteService"

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        options: DrivingRouteOptions,
    ): Result<DrivingRouteResult> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("OPENROUTESERVICE_API_KEY missing"))
        }
        runCatching {
            val profile = when (options.vehicleMode) {
                VehicleRoutingMode.TRUCK -> "driving-hgv"
                VehicleRoutingMode.CAR -> "driving-car"
            }
            val url = "$ORS_BASE/v2/directions/$profile"
            val coordinates = JSONArray()
                .put(JSONArray().put(origin.lng).put(origin.lat))
                .put(JSONArray().put(destination.lng).put(destination.lat))
            val bodyJson = JSONObject()
                .put("coordinates", coordinates)
            if (options.vehicleMode == VehicleRoutingMode.TRUCK) {
                val spec = options.truckSpec
                val restrictions = JSONObject()
                    .put("height", spec.heightMeters)
                    .put("width", spec.widthMeters)
                    .put("length", spec.lengthMeters)
                    .put("weight", spec.weightTons * 1000.0)
                    .put("axleload", spec.axleLoadTons * 1000.0)
                    .put("hazmat", spec.hazardousMaterial)
                spec.tunnelCategory?.takeIf { it.isNotBlank() }?.let {
                    restrictions.put("tunnel_category", it)
                }
                bodyJson.put(
                    "options",
                    JSONObject().put(
                        "profile_params",
                        JSONObject().put("restrictions", restrictions),
                    ),
                )
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("ORS HTTP ${resp.code}: $body")
                DrivingRouteResult(parseOrsBody(body), providerLabel)
            }
        }
    }

    internal fun parseOrsBody(body: String): List<LatLngPoint> {
        val json = JSONObject(body)
        val routes = json.optJSONArray("routes") ?: error("ORS: no routes array")
        if (routes.length() == 0) error("ORS: empty routes")
        val encoded = routes.getJSONObject(0).optString("geometry")
        val points = EncodedPolylineCodec.decode(encoded)
        if (points.size < 2) error("ORS: polyline too short")
        return points
    }

    companion object {
        private const val ORS_BASE = "https://api.openrouteservice.org"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
