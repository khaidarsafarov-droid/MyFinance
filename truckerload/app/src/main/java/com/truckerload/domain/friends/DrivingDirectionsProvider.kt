package com.truckerload.domain.friends

/**
 * Abstraction over a driving / truck directions backend.
 * Keeps [RoadRouteSession] free of Android / OkHttp dependencies.
 */
interface DrivingDirectionsProvider {
    fun isConfigured(): Boolean

    suspend fun fetchRoute(request: RouteRequest): Result<RoadRouteResult>

    /** Convenience for callers that only need origin → destination. */
    suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
    ): Result<List<LatLngPoint>> = fetchRoute(
        RouteRequest(origin = origin, destination = destination),
    ).mapCatching { result ->
        if (result.points.size < 2) error(result.failureReason ?: "empty route")
        result.points
    }
}
