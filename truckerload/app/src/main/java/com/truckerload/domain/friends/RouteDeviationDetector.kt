package com.truckerload.domain.friends

import kotlin.math.max
import kotlin.math.min

/**
 * Detects when the driver has left the planned road route so we can re-fetch directions.
 */
object RouteDeviationDetector {

    /** Re-route when the driver is farther than this from the planned polyline (km). */
    const val DEVIATION_THRESHOLD_KM = 0.5

    /**
     * Minimum distance from [point] to any segment of [polyline] (km).
     * Returns [Double.POSITIVE_INFINITY] when [polyline] has fewer than 2 points.
     */
    fun distanceToPolylineKm(point: LatLngPoint, polyline: List<LatLngPoint>): Double {
        if (polyline.size < 2) return Double.POSITIVE_INFINITY
        var min = Double.POSITIVE_INFINITY
        for (i in 0 until polyline.lastIndex) {
            val d = distanceToSegmentKm(point, polyline[i], polyline[i + 1])
            if (d < min) min = d
        }
        return min
    }

    fun isDeviated(
        current: LatLngPoint,
        routePolyline: List<LatLngPoint>,
        thresholdKm: Double = DEVIATION_THRESHOLD_KM,
    ): Boolean = distanceToPolylineKm(current, routePolyline) > thresholdKm

    /**
     * Nearest point on the segment [a]–[b] to [p], using a flat-earth approximation
     * (accurate enough for deviation checks on US highway scales).
     */
    internal fun distanceToSegmentKm(p: LatLngPoint, a: LatLngPoint, b: LatLngPoint): Double {
        val ax = a.lng
        val ay = a.lat
        val bx = b.lng
        val by = b.lat
        val px = p.lng
        val py = p.lat
        val dx = bx - ax
        val dy = by - ay
        if (dx == 0.0 && dy == 0.0) {
            return RouteIntersectionMatcher.haversineKm(p, a)
        }
        val t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
        val proj = LatLngPoint(ay + t * dy, ax + t * dx)
        return RouteIntersectionMatcher.haversineKm(p, proj)
    }
}
