package com.truckerload.domain.friends

/**
 * Geometry helpers for road polylines: off-route detection and trimming
 * the remaining path so the blue line starts near the driver's GPS.
 */
object RoadRouteGeometry {

    /** Off-route threshold before reroute is considered (~50 m per product spec). */
    const val DEFAULT_OFF_ROUTE_METERS = 50.0

    /** Driver must stay off the corridor this long before a reroute fires. */
    const val DEFAULT_OFF_ROUTE_DURATION_MS = 10_000L

    fun isOffRoute(
        point: LatLngPoint,
        route: List<LatLngPoint>,
        thresholdMeters: Double = DEFAULT_OFF_ROUTE_METERS,
    ): Boolean {
        if (route.size < 2) return true
        return minDistanceMeters(point, route) > thresholdMeters
    }

    fun minDistanceMeters(point: LatLngPoint, route: List<LatLngPoint>): Double {
        if (route.isEmpty()) return Double.POSITIVE_INFINITY
        if (route.size == 1) return RouteIntersectionMatcher.haversineKm(point, route[0]) * 1000.0
        var min = Double.POSITIVE_INFINITY
        for (i in 0 until route.lastIndex) {
            val d = distanceToSegmentMeters(point, route[i], route[i + 1])
            if (d < min) min = d
        }
        return min
    }

    /**
     * Index of the polyline vertex nearest [point] (by chord distance to segments,
     * returning the farther endpoint of the closest segment when projection is mid-edge).
     */
    fun nearestVertexIndex(point: LatLngPoint, route: List<LatLngPoint>): Int {
        if (route.isEmpty()) return -1
        if (route.size == 1) return 0
        var bestIdx = 0
        var bestDist = Double.POSITIVE_INFINITY
        for (i in 0 until route.lastIndex) {
            val a = route[i]
            val b = route[i + 1]
            val t = projectionT(point, a, b)
            val candidateIdx = if (t < 0.5) i else i + 1
            val d = distanceToSegmentMeters(point, a, b)
            if (d < bestDist) {
                bestDist = d
                bestIdx = candidateIdx
            }
        }
        return bestIdx
    }

    /**
     * Remaining road points from near [current] to the end of [route].
     * Always starts with [current] so the polyline attaches to the green marker.
     */
    fun remainingFromCurrent(route: List<LatLngPoint>, current: LatLngPoint): List<LatLngPoint> {
        if (route.isEmpty()) return listOf(current)
        val idx = nearestVertexIndex(current, route).coerceAtLeast(0)
        val ahead = route.subList(idx, route.size)
        return buildList {
            add(current)
            for (p in ahead) {
                if (p.lat != current.lat || p.lng != current.lng) add(p)
            }
        }
    }

    fun samePoint(a: LatLngPoint?, b: LatLngPoint?, epsMeters: Double = 80.0): Boolean {
        if (a == null || b == null) return a == null && b == null
        return RouteIntersectionMatcher.haversineKm(a, b) * 1000.0 <= epsMeters
    }

    private fun distanceToSegmentMeters(p: LatLngPoint, a: LatLngPoint, b: LatLngPoint): Double {
        val t = projectionT(p, a, b).coerceIn(0.0, 1.0)
        val proj = LatLngPoint(
            lat = a.lat + (b.lat - a.lat) * t,
            lng = a.lng + (b.lng - a.lng) * t,
        )
        return RouteIntersectionMatcher.haversineKm(p, proj) * 1000.0
    }

    /** Parametric t for projection of p onto segment a→b in lat/lng space. */
    private fun projectionT(p: LatLngPoint, a: LatLngPoint, b: LatLngPoint): Double {
        val dx = b.lng - a.lng
        val dy = b.lat - a.lat
        val len2 = dx * dx + dy * dy
        if (len2 < 1e-18) return 0.0
        return ((p.lng - a.lng) * dx + (p.lat - a.lat) * dy) / len2
    }
}
