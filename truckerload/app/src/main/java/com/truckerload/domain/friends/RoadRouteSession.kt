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
    private val offRouteDurationMs: Long = RoadRouteGeometry.DEFAULT_OFF_ROUTE_DURATION_MS,
    private val minRerouteIntervalMs: Long = DEFAULT_REROUTE_INTERVAL_MS,
    private val routeOptions: DrivingRouteOptions = DrivingRouteOptions(),
) {
    private data class CachedRoute(
        val destination: LatLngPoint,
        val fetchOrigin: LatLngPoint,
        val points: List<LatLngPoint>,
        val fetchedAtMs: Long,
        val providerLabel: String?,
    )

    private val cache = mutableMapOf<String, CachedRoute>()
    private val offRouteSinceMs = mutableMapOf<String, Long>()

    fun clear(cacheKey: String? = null) {
        if (cacheKey == null) {
            cache.clear()
            offRouteSinceMs.clear()
        } else {
            cache.remove(cacheKey)
            offRouteSinceMs.remove(cacheKey)
        }
    }

    /**
     * Returns a road polyline from near [currentOrStart] to [destination].
     * Falls back to a 2-point straight segment when all directions backends fail.
     */
    suspend fun remainingRoad(
        cacheKey: String,
        currentOrStart: LatLngPoint?,
        destination: LatLngPoint?,
        nowMs: Long = System.currentTimeMillis(),
    ): RoadRouteOutcome {
        if (destination == null || currentOrStart == null) {
            return straightOutcome(currentOrStart, destination)
        }
        val existing = cache[cacheKey]
        val destChanged = existing != null &&
            !RoadRouteGeometry.samePoint(existing.destination, destination, epsMeters = 150.0)
        if (destChanged) {
            cache.remove(cacheKey)
            offRouteSinceMs.remove(cacheKey)
        }

        val cached = cache[cacheKey]
        val offRoute = cached != null &&
            RoadRouteGeometry.isOffRoute(currentOrStart, cached.points, offRouteThresholdMeters)
        if (offRoute) {
            offRouteSinceMs.putIfAbsent(cacheKey, nowMs)
        } else {
            offRouteSinceMs.remove(cacheKey)
        }
        val sustainedOffRoute = offRoute &&
            (nowMs - (offRouteSinceMs[cacheKey] ?: nowMs)) >= offRouteDurationMs

        val needFetch = when {
            !directions.isConfigured() -> false
            cached == null -> true
            sustainedOffRoute -> nowMs - cached.fetchedAtMs >= minRerouteIntervalMs
            else -> false
        }

        if (needFetch) {
            val result = directions.fetchDrivingRoute(currentOrStart, destination, routeOptions)
            val route = result.getOrNull()
            if (route != null && route.points.size >= 2) {
                cache[cacheKey] = CachedRoute(
                    destination = destination,
                    fetchOrigin = currentOrStart,
                    points = route.points,
                    fetchedAtMs = nowMs,
                    providerLabel = route.providerLabel,
                )
                offRouteSinceMs.remove(cacheKey)
            }
        }

        val hit = cache[cacheKey]
        if (hit != null && hit.points.size >= 2) {
            return RoadRouteOutcome(
                points = RoadRouteGeometry.remainingFromCurrent(hit.points, currentOrStart),
                isRoadRouted = true,
                providerLabel = hit.providerLabel,
            )
        }
        return straightOutcome(currentOrStart, destination)
    }

    private fun straightOutcome(start: LatLngPoint?, dest: LatLngPoint?): RoadRouteOutcome =
        RoadRouteOutcome(
            points = straight(start, dest),
            isRoadRouted = false,
            providerLabel = null,
        )

    private fun straight(start: LatLngPoint?, dest: LatLngPoint?): List<LatLngPoint> =
        buildList {
            if (start != null) add(start)
            if (dest != null && (start == null || dest.lat != start.lat || dest.lng != start.lng)) {
                add(dest)
            }
        }

    companion object {
        const val DEFAULT_REROUTE_INTERVAL_MS = 5_000L
        const val SELF_CACHE_KEY = "__me__"
    }
}
