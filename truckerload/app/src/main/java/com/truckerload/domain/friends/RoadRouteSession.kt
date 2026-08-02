package com.truckerload.domain.friends

/**
 * Caches driving polylines and refreshes them when the driver leaves the
 * road corridor (distance + sustained duration) or the destination changes.
 *
 * Keyed by [cacheKey] so the friends map can keep one session for "me" and one
 * per friend without cross-contaminating routes.
 */
class RoadRouteSession(
    private val directions: DrivingDirectionsProvider,
    private val offRouteThresholdMeters: Double = RoadRouteGeometry.DEFAULT_OFF_ROUTE_METERS,
    private val minOffRouteDurationMs: Long = DEFAULT_OFF_ROUTE_DURATION_MS,
    private val minRerouteIntervalMs: Long = DEFAULT_REROUTE_INTERVAL_MS,
    private val profile: VehicleRoutingProfile = VehicleRoutingProfile.TRUCK,
) {
    private data class CachedRoute(
        val destination: LatLngPoint,
        val fetchOrigin: LatLngPoint,
        val points: List<LatLngPoint>,
        val source: String,
        val fetchedAtMs: Long,
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
     * Falls back to a 2-point straight segment when Directions is unavailable.
     */
    suspend fun remainingRoad(
        cacheKey: String,
        currentOrStart: LatLngPoint?,
        destination: LatLngPoint?,
        nowMs: Long = System.currentTimeMillis(),
    ): RoadPathResult {
        if (destination == null || currentOrStart == null) {
            return RoadPathResult(
                points = straight(currentOrStart, destination),
                isRoadNetwork = false,
                source = SOURCE_STRAIGHT,
            )
        }
        val existing = cache[cacheKey]
        val destChanged = existing != null &&
            !RoadRouteGeometry.samePoint(existing.destination, destination, epsMeters = 150.0)
        if (destChanged) {
            cache.remove(cacheKey)
            offRouteSinceMs.remove(cacheKey)
        }

        val cached = cache[cacheKey]
        val needFetch = when {
            !directions.isConfigured() -> false
            cached == null -> true
            else -> shouldReroute(cacheKey, currentOrStart, cached, nowMs)
        }

        if (needFetch) {
            val result = directions.fetchDrivingRoute(currentOrStart, destination, profile)
            val road = result.getOrNull()
            if (road != null && road.points.size >= 2) {
                cache[cacheKey] = CachedRoute(
                    destination = destination,
                    fetchOrigin = currentOrStart,
                    points = road.points,
                    source = road.source,
                    fetchedAtMs = nowMs,
                )
                offRouteSinceMs.remove(cacheKey)
            }
        }

        val road = cache[cacheKey]
        if (road != null && road.points.size >= 2) {
            return RoadPathResult(
                points = RoadRouteGeometry.remainingFromCurrent(road.points, currentOrStart),
                isRoadNetwork = true,
                source = road.source,
            )
        }
        return RoadPathResult(
            points = straight(currentOrStart, destination),
            isRoadNetwork = false,
            source = SOURCE_STRAIGHT,
        )
    }

    private fun shouldReroute(
        cacheKey: String,
        current: LatLngPoint,
        cached: CachedRoute,
        nowMs: Long,
    ): Boolean {
        val off = RoadRouteGeometry.isOffRoute(current, cached.points, offRouteThresholdMeters)
        if (!off) {
            offRouteSinceMs.remove(cacheKey)
            return false
        }
        val since = offRouteSinceMs.getOrPut(cacheKey) { nowMs }
        val sustained = nowMs - since >= minOffRouteDurationMs
        val cooledDown = nowMs - cached.fetchedAtMs >= minRerouteIntervalMs
        return sustained && cooledDown
    }

    private fun straight(start: LatLngPoint?, dest: LatLngPoint?): List<LatLngPoint> =
        buildList {
            if (start != null) add(start)
            if (dest != null && (start == null || dest.lat != start.lat || dest.lng != start.lng)) {
                add(dest)
            }
        }

    companion object {
        /** ~0.06 mi — matches acceptance “100+ m off route”, GPS-tolerant vs 50 m. */
        const val DEFAULT_OFF_ROUTE_DURATION_MS = 10_000L
        /** Allow a fresh Directions/OSRM call a few seconds after sustained deviation. */
        const val DEFAULT_REROUTE_INTERVAL_MS = 5_000L
        const val SELF_CACHE_KEY = "__me__"
        const val SOURCE_STRAIGHT = "straight"
    }
}
