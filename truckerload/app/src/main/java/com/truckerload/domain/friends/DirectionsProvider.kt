package com.truckerload.domain.friends

/**
 * Fetches driving directions between two points (road-following geometry).
 */
fun interface DirectionsProvider {
    suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
    ): List<LatLngPoint>?
}
