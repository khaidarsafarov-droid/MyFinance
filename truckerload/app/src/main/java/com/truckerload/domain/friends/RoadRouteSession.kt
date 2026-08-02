package com.truckerload.domain.friends

/**
 * Caches road polylines and refreshes them when the driver leaves the
 * road corridor for long enough, or the destination changes.
 *
 * Keyed by [cacheKey] so the friends map can keep one session for "me" and one
 * per friend without cross-contaminating routes.
 *
 * Reroute rule (acceptance): distance to corridor > [offRouteThresholdMeters]
 * continuously for [offRouteHoldMs] (defaults 50 m / 10 s).
 */
class RoadRouteSession(
    private val directions: DrivingDirectionsProvider,
    private val offRouteThresholdMeters: Double = RoadRouteGeometry.DEFAULT_OFF_ROUTE_METERS,
    private val offRouteHoldMs: Long = DEFAULT_OFF_ROUTE_HOLD_MS,
    private val vehicleMode: VehicleRoutingMode = VehicleRoutingMode.TRUCK,
    private val truckParams: TruckRoutingParams = TruckRoutingParams(),
) {
    private data class CachedRoute(
        val destination: LatLngPoint,
        val fetchOrigin: LatLngPoint,
        val points: List<LatLngPoint>,
        val fetchedAtMs: Long,
        val isRoadNetwork: Boolean,
        val distanceMeters: Long?,
        val durationSeconds: Long?,
        val providerName: String?,
        val offRouteSinceMs: Long? = null,
    )

    private val cache = mutableMapOf<String, CachedRoute>()
    private val lastFailures = mutableMapOf<String, String?>()
    private val lastFetchAttemptMs = mutableMapOf<String, Long>()

    fun clear(cacheKey: String? = null) {
        if (cacheKey == null) {
            cache.clear()
            lastFailures.clear()
            lastFetchAttemptMs.clear()
        } else {
            cache.remove(cacheKey)
            lastFailures.remove(cacheKey)
            lastFetchAttemptMs.remove(cacheKey)
        }
    }

    fun lastFailureReason(cacheKey: String): String? = lastFailures[cacheKey]

    /**
     * Returns a road polyline from near [currentOrStart] to [destination].
     * Falls back to a 2-point straight segment when Directions is unavailable.
     */
    suspend fun remainingRoad(
        cacheKey: String,
        currentOrStart: LatLngPoint?,
        destination: LatLngPoint?,
        nowMs: Long = System.currentTimeMillis(),
    ): List<LatLngPoint> = remainingRoadResult(
        cacheKey = cacheKey,
        currentOrStart = currentOrStart,
        destination = destination,
        nowMs = nowMs,
    ).points

    suspend fun remainingRoadResult(
        cacheKey: String,
        currentOrStart: LatLngPoint?,
        destination: LatLngPoint?,
        nowMs: Long = System.currentTimeMillis(),
    ): RoadRouteResult {
        if (destination == null || currentOrStart == null) {
            val points = straight(currentOrStart, destination)
            return RoadRouteResult(
                points = points,
                isRoadNetwork = false,
                failureReason = "missing_endpoints",
            )
        }
        val existing = cache[cacheKey]
        val destChanged = existing != null &&
            !RoadRouteGeometry.samePoint(existing.destination, destination, epsMeters = 150.0)
        if (destChanged) cache.remove(cacheKey)

        var cached = cache[cacheKey]
        if (cached != null) {
            val off = RoadRouteGeometry.isOffRoute(
                currentOrStart,
                cached.points,
                offRouteThresholdMeters,
            )
            cached = if (off) {
                val since = cached.offRouteSinceMs ?: nowMs
                cached.copy(offRouteSinceMs = since)
            } else {
                cached.copy(offRouteSinceMs = null)
            }
            cache[cacheKey] = cached
        }

        val snapshot = cached
        val offRouteSince = snapshot?.offRouteSinceMs
        val lastAttempt = lastFetchAttemptMs[cacheKey]
        val retryReady = lastAttempt == null || nowMs - lastAttempt >= offRouteHoldMs
        val needFetch = when {
            !directions.isConfigured() -> false
            !retryReady -> false
            snapshot == null -> true
            offRouteSince != null && nowMs - offRouteSince >= offRouteHoldMs -> true
            else -> false
        }

        if (needFetch) {
            lastFetchAttemptMs[cacheKey] = nowMs
            val request = RouteRequest(
                origin = currentOrStart,
                destination = destination,
                vehicleMode = vehicleMode,
                truck = truckParams,
            )
            val result = directions.fetchRoute(request)
            val road = result.getOrNull()
            if (road != null && road.points.size >= 2 && road.isRoadNetwork) {
                cache[cacheKey] = CachedRoute(
                    destination = destination,
                    fetchOrigin = currentOrStart,
                    points = road.points,
                    fetchedAtMs = nowMs,
                    isRoadNetwork = true,
                    distanceMeters = road.distanceMeters,
                    durationSeconds = road.durationSeconds,
                    providerName = road.providerName,
                    offRouteSinceMs = null,
                )
                lastFailures.remove(cacheKey)
            } else {
                lastFailures[cacheKey] = result.exceptionOrNull()?.message
                    ?: road?.failureReason
                    ?: "directions_failed"
            }
        } else if (!directions.isConfigured()) {
            lastFailures[cacheKey] = "directions_unconfigured"
        }

        val road = cache[cacheKey]
        if (road != null && road.points.size >= 2) {
            val remaining = RoadRouteGeometry.remainingFromCurrent(road.points, currentOrStart)
            return RoadRouteResult(
                points = remaining,
                isRoadNetwork = road.isRoadNetwork,
                distanceMeters = road.distanceMeters,
                durationSeconds = road.durationSeconds,
                providerName = road.providerName,
                failureReason = if (road.isRoadNetwork) null else lastFailures[cacheKey],
            )
        }
        val reason = lastFailures[cacheKey] ?: "directions_unavailable"
        return RoadRouteResult.straight(currentOrStart, destination, reason)
    }

    private fun straight(start: LatLngPoint?, dest: LatLngPoint?): List<LatLngPoint> =
        buildList {
            if (start != null) add(start)
            if (dest != null && (start == null || dest.lat != start.lat || dest.lng != start.lng)) {
                add(dest)
            }
        }

    companion object {
        /** Continuous off-route hold before recalculating (acceptance: 10 s). */
        const val DEFAULT_OFF_ROUTE_HOLD_MS = 10_000L

        /** @deprecated Use [DEFAULT_OFF_ROUTE_HOLD_MS]. Kept for older call sites. */
        @Deprecated("Use DEFAULT_OFF_ROUTE_HOLD_MS", ReplaceWith("DEFAULT_OFF_ROUTE_HOLD_MS"))
        const val DEFAULT_REROUTE_INTERVAL_MS = DEFAULT_OFF_ROUTE_HOLD_MS

        const val SELF_CACHE_KEY = "__me__"
    }
}
