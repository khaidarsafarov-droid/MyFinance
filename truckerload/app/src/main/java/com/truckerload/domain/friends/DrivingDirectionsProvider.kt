package com.truckerload.domain.friends

/**
 * Abstraction over a driving directions backend (Google Directions, etc.).
 * Keeps [RoadRouteSession] free of Android / OkHttp dependencies.
 */
interface DrivingDirectionsProvider {
    fun isConfigured(): Boolean

    suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
    ): Result<List<LatLngPoint>>
}
