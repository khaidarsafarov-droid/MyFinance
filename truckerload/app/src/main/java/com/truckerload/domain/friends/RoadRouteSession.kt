package com.truckerload.domain.friends

/**
 * Caches driving polylines and refreshes them when the driver leaves the
 * road corridor or the destination changes.
 *
 * Keyed by [cacheKey] so the friends map can keep one session for "me" and one
 * per friend without cross-contaminating routes.
 */
class RoadRouteSession(
    private val directions: DrivingDirectionsProvider,
    private val offRouteThresholdMeters: Double = RoadRouteGeometry.DEFAULT_OFF_ROUTE_METERS,
    private val minRerouteIntervalMs: Long = DEFAULT_REROUTE_INTERVAL_MS,
) {
    private data class CachedRoute(
        val destination: LatLngPoint,
        val fetchOrigin: LatLngPoint,
        val points: List<LatLngPoint>,
        val fetchedAtMs: Long,
    )

    private val cache = mutableMapOf<String, CachedRoute>()

    fun clear(cacheKey: String? = null) {
        if (cacheKey == null) cache.clear() else cache.remove(cacheKey)
    }

    /**
     * Returns a road polyline from near [currentOrStart] to [destination].
     * Falls back to a 2-point straight segment when Directions is unavailable.
     */
    suspend fun remainingRoad(
        cacheKey: String,
        currentOrStart: LatLngPoint?,
        destination: LatLngPoint?,
        nowMs: Long = System.currentTimeMillis(),
    ): List<LatLngPoint> {
        if (destination == null || currentOrStart == null) {
            return straight(currentOrStart, destination)
        }
        val existing = cache[cacheKey]
        val destChanged = existing != null &&
            !RoadRouteGeometry.samePoint(existing.destination, destination, epsMeters = 150.0)
        if (destChanged) cache.remove(cacheKey)

        val cached = cache[cacheKey]
        val needFetch = when {
            !directions.isConfigured() -> false
            cached == null -> true
            RoadRouteGeometry.isOffRoute(currentOrStart, cached.points, offRouteThresholdMeters) -> {
                nowMs - cached.fetchedAtMs >= minRerouteIntervalMs
            }
            else -> false
        }

        if (needFetch) {
            val result = directions.fetchDrivingRoute(currentOrStart, destination)
            val road = result.getOrNull()
            if (road != null && road.size >= 2) {
                cache[cacheKey] = CachedRoute(
                    destination = destination,
                    fetchOrigin = currentOrStart,
                    points = road,
                    fetchedAtMs = nowMs,
                )
            }
        }

        val road = cache[cacheKey]?.points
        if (road != null && road.size >= 2) {
            return RoadRouteGeometry.remainingFromCurrent(road, currentOrStart)
        }
        return straight(currentOrStart, destination)
    }

    private fun straight(start: LatLngPoint?, dest: LatLngPoint?): List<LatLngPoint> =
        buildList {
            if (start != null) add(start)
            if (dest != null && (start == null || dest.lat != start.lat || dest.lng != start.lng)) {
                add(dest)
            }
        }

    companion object {
        const val DEFAULT_REROUTE_INTERVAL_MS = 45_000L
        const val SELF_CACHE_KEY = "__me__"
    }
}
