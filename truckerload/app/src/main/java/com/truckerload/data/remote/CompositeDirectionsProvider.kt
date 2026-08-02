package com.truckerload.data.remote

import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.RoadRouteResult
import com.truckerload.domain.friends.RouteRequest

/**
 * Tries providers in order and returns the first road-network polyline.
 * Default chain: Google Directions (truck-aware avoidances) → OSRM road fallback.
 */
class CompositeDirectionsProvider(
    private val providers: List<DrivingDirectionsProvider>,
) : DrivingDirectionsProvider {
    constructor(
        google: DrivingDirectionsProvider = GoogleDirectionsClient(),
        osrm: DrivingDirectionsProvider = OsrmDirectionsClient(),
    ) : this(listOf(google, osrm))

    override fun isConfigured(): Boolean = providers.any { it.isConfigured() }

    override suspend fun fetchRoute(request: RouteRequest): Result<RoadRouteResult> {
        var lastFailure: Throwable? = null
        for (provider in providers) {
            if (!provider.isConfigured()) continue
            val result = provider.fetchRoute(request)
            val route = result.getOrNull()
            if (route != null && route.points.size >= 2 && route.isRoadNetwork) {
                return Result.success(route)
            }
            lastFailure = result.exceptionOrNull()
                ?: IllegalStateException(route?.failureReason ?: "empty route")
        }
        return Result.failure(
            lastFailure ?: IllegalStateException("No directions provider configured"),
        )
    }
}
