package com.truckerload.domain.friends

import com.truckerload.data.remote.GoogleDirectionsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enriches straight-line "remaining" segments with road geometry from Google Directions,
 * caches results, and re-fetches when the driver deviates from the planned path.
 */
@Singleton
class RoadRouteResolver @Inject constructor(
    private val directionsService: DirectionsProvider,
) {

    data class CachedRoute(
        val origin: LatLngPoint,
        val destination: LatLngPoint,
        val points: List<LatLngPoint>,
        val fetchedAtMillis: Long = System.currentTimeMillis(),
    )

    private val cache = mutableMapOf<String, CachedRoute>()

    /**
     * Replaces the straight [split.remaining] segment with a road-following polyline.
     * Falls back to the straight line when Directions is unavailable.
     */
    suspend fun enrichRemaining(
        split: FriendRoutePolylineBuilder.SplitPolylines,
        current: LatLngPoint?,
        cacheKey: String,
    ): List<LatLngPoint> {
        val start = split.remaining.firstOrNull() ?: return split.remaining
        val dest = split.remaining.lastOrNull() ?: return split.remaining
        if (start.lat == dest.lat && start.lng == dest.lng) return split.remaining

        val cached = cache[cacheKey]
        if (cached != null && cached.destination == dest) {
            val needsRecalc = current != null &&
                RouteDeviationDetector.isDeviated(current, cached.points)
            if (!needsRecalc) {
                return trimRouteFromCurrent(cached.points, current ?: start)
            }
        }

        val roadPoints = directionsService.fetchDrivingRoute(start, dest)
            ?: return split.remaining

        val entry = CachedRoute(origin = start, destination = dest, points = roadPoints)
        cache[cacheKey] = entry
        return trimRouteFromCurrent(roadPoints, current ?: start)
    }

    fun clearCache() {
        cache.clear()
    }

    /**
     * Drops points already driven so the blue line starts at the driver's position.
     */
    internal fun trimRouteFromCurrent(
        route: List<LatLngPoint>,
        current: LatLngPoint,
    ): List<LatLngPoint> {
        if (route.size < 2) return route
        var nearestIdx = 0
        var nearestDist = Double.POSITIVE_INFINITY
        for (i in route.indices) {
            val d = RouteIntersectionMatcher.haversineKm(current, route[i])
            if (d < nearestDist) {
                nearestDist = d
                nearestIdx = i
            }
        }
        val trimmed = buildList {
            add(current)
            addAll(route.drop(nearestIdx + 1))
        }.distinctConsecutive()
        return trimmed.takeIf { it.size >= 2 } ?: listOf(current, route.last())
    }

    private fun List<LatLngPoint>.distinctConsecutive(): List<LatLngPoint> {
        if (isEmpty()) return this
        val out = ArrayList<LatLngPoint>(size)
        for (p in this) {
            val last = out.lastOrNull()
            if (last == null || last.lat != p.lat || last.lng != p.lng) out.add(p)
        }
        return out
    }
}
