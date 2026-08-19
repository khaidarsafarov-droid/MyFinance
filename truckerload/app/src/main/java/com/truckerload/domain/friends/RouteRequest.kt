package com.truckerload.domain.friends

/**
 * Vehicle profile for road routing.
 *
 * Google Directions has no true `mode=truck`. For trucks we request driving
 * directions with truck-oriented avoidances and carry the full vehicle
 * dimensions so a production TomTom / HERE truck backend can consume them.
 */
enum class VehicleRoutingMode {
    TRUCK,
    CAR,
}

data class TruckRoutingParams(
    val heightMeters: Double = 4.0,
    val widthMeters: Double = 2.6,
    val lengthMeters: Double = 16.15,
    val weightTons: Double = 36.0,
    val axleLoadTons: Double = 10.0,
    val hazardousMaterial: Boolean = false,
    /** ADR tunnel category when known (B/C/D/E). */
    val tunnelCategory: String? = null,
)

data class RouteRequest(
    val origin: LatLngPoint,
    val destination: LatLngPoint,
    val vehicleMode: VehicleRoutingMode = VehicleRoutingMode.TRUCK,
    val truck: TruckRoutingParams = TruckRoutingParams(),
)

data class RoadRouteResult(
    val points: List<LatLngPoint>,
    val isRoadNetwork: Boolean,
    val distanceMeters: Long? = null,
    val durationSeconds: Long? = null,
    val providerName: String? = null,
    val failureReason: String? = null,
    val traveledPoints: List<LatLngPoint> = emptyList(),
) {
    companion object {
        fun straight(start: LatLngPoint, destination: LatLngPoint, reason: String?): RoadRouteResult =
            RoadRouteResult(
                points = listOf(start, destination).distinct(),
                isRoadNetwork = false,
                failureReason = reason,
            )
    }
}
