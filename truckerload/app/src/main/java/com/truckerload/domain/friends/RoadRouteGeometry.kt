package com.truckerload.domain.friends

import kotlin.math.min

/**
 * Helpers for road-following polylines: nearest-point trim and off-route detection.
 */
object RoadRouteGeometry {

    /** Default: ~0.5 mi — replan when the driver leaves the cached road corridor. */
    const val DEFAULT_OFF_ROUTE_METERS = 800.0

    fun haversineMeters(a: LatLngPoint, b: LatLngPoint): Double =
        RouteIntersectionMatcher.haversineKm(a, b) * 1000.0

    /**
     * Index of the path vertex closest to [point].
     * Returns -1 when [path] is empty.
     */
    fun nearestVertexIndex(path: List<LatLngPoint>, point: LatLngPoint): Int {
        if (path.isEmpty()) return -1
        var bestIdx = 0
        var best = Double.MAX_VALUE
        for (i in path.indices) {
            val d = haversineMeters(path[i], point)
            if (d < best) {
                best = d
                bestIdx = i
            }
        }
        return bestIdx
    }

    /**
     * Remaining road geometry from the vertex nearest [current] through the end.
     * Prepends [current] so the blue line starts at the driver's pin.
     */
    fun remainingFromNearest(
        roadPath: List<LatLngPoint>,
        current: LatLngPoint?,
    ): List<LatLngPoint> {
        if (roadPath.size < 2) return emptyList()
        val start = current ?: return roadPath.distinctConsecutive()
        val idx = nearestVertexIndex(roadPath, start).coerceAtLeast(0)
        // Skip a short prefix already behind the driver so the line doesn't loop back.
        val from = min(idx + 1, roadPath.lastIndex)
        return buildList {
            add(start)
            for (i in from until roadPath.size) add(roadPath[i])
        }.distinctConsecutive()
    }

    fun isOffRoute(
        roadPath: List<LatLngPoint>,
        current: LatLngPoint,
        thresholdMeters: Double = DEFAULT_OFF_ROUTE_METERS,
    ): Boolean {
        if (roadPath.size < 2) return true
        val idx = nearestVertexIndex(roadPath, current)
        if (idx < 0) return true
        return haversineMeters(roadPath[idx], current) > thresholdMeters
    }

    /** Bucket coords (~110 m) so GPS jitter does not thrash Directions. */
    fun cacheBucket(point: LatLngPoint): String {
        val lat = (point.lat * 1000.0).toInt() / 1000.0
        val lng = (point.lng * 1000.0).toInt() / 1000.0
        return "$lat,$lng"
    }

    fun routeCacheKey(origin: LatLngPoint, destination: LatLngPoint): String =
        "${cacheBucket(origin)}→${cacheBucket(destination)}"

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
