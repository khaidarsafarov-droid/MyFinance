package com.truckerload.domain.friends

/**
 * Abstraction over a driving directions backend (Google Directions, OSRM, etc.).
 * Keeps [RoadRouteSession] free of Android / OkHttp dependencies.
 */
interface DrivingDirectionsProvider {
    fun isConfigured(): Boolean

    /**
     * Road-network polyline from [origin] to [destination].
     * Must not return an air/straight chord — callers fall back to that themselves.
     */
    suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        profile: VehicleRoutingProfile = VehicleRoutingProfile.TRUCK,
    ): Result<RouteFetchResult>
}

enum class VehicleRoutingProfile {
    /** Passenger car / light vehicle. */
    CAR,

    /**
     * Heavy truck. Prefer a truck-aware engine when available.
     * Google classic Directions has no full truck constraints — see client comments.
     */
    TRUCK,
}

data class RouteFetchResult(
    val points: List<LatLngPoint>,
    /** Stable id for UI/logs: "google", "osrm", … */
    val source: String,
)

data class RoadPathResult(
    val points: List<LatLngPoint>,
    /** True when points came from a road-routing API (not a 2-point chord). */
    val isRoadNetwork: Boolean,
    val source: String? = null,
)
