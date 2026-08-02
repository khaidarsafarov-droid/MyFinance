package com.truckerload.domain.friends

/** Result of a road-route lookup (road polyline or straight-line fallback). */
data class RoadRouteOutcome(
    val points: List<LatLngPoint>,
    /** True when a directions backend returned a road-following polyline. */
    val isRoadRouted: Boolean,
    /** Human-readable provider label for UI/debug (e.g. "Google", "OSRM"). */
    val providerLabel: String?,
)
