package com.truckerload.data.remote

import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RouteFetchResult
import com.truckerload.domain.friends.VehicleRoutingProfile

/**
 * Tries [primary] (usually Google Directions), then [fallback] (OSRM).
 * Ensures the friends map still gets a road polyline when the Maps SDK key
 * cannot call the Directions HTTP API (common Android package restriction).
 */
class CompositeDrivingDirectionsProvider(
    private val primary: DrivingDirectionsProvider,
    private val fallback: DrivingDirectionsProvider,
) : DrivingDirectionsProvider {
    override fun isConfigured(): Boolean =
        primary.isConfigured() || fallback.isConfigured()

    override suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        profile: VehicleRoutingProfile,
    ): Result<RouteFetchResult> {
        var lastError: Throwable? = null
        if (primary.isConfigured()) {
            val result = primary.fetchDrivingRoute(origin, destination, profile)
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
            // android.util.Log can throw in plain JVM unit tests — keep routing working.
            runCatching {
                android.util.Log.w(TAG, "primary directions failed, trying fallback", lastError)
            }
        }
        if (fallback.isConfigured()) {
            val result = fallback.fetchDrivingRoute(origin, destination, profile)
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull() ?: lastError
        }
        return Result.failure(
            lastError ?: IllegalStateException("No directions provider configured"),
        )
    }

    companion object {
        private const val TAG = "TL.Directions"

        /** Default stack: Google (optional key) → OSRM public demo (always on). */
        fun default(): CompositeDrivingDirectionsProvider =
            CompositeDrivingDirectionsProvider(
                primary = GoogleDirectionsClient(),
                fallback = OsrmDirectionsClient(),
            )
    }
}
