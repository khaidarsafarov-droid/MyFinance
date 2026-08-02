package com.truckerload.domain.friends

/** Road polyline returned by a directions backend. */
data class DrivingRouteResult(
    val points: List<LatLngPoint>,
    val providerLabel: String,
)

/**
 * Abstraction over a driving directions backend (Google Directions, OSRM, etc.).
 * Keeps [RoadRouteSession] free of Android / OkHttp dependencies.
 */
interface DrivingDirectionsProvider {
    /** Short label shown in the friends map when routing succeeds. */
    val providerLabel: String

    fun isConfigured(): Boolean

    suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        options: DrivingRouteOptions = DrivingRouteOptions(),
    ): Result<DrivingRouteResult>
}
