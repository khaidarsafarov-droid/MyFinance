package com.truckerload.domain.friends

/** Options passed to directions backends (friends map uses OSRM driving only). */
data class DrivingRouteOptions(
    val vehicleMode: VehicleRoutingMode = VehicleRoutingMode.CAR,
)

enum class VehicleRoutingMode {
    CAR,
    TRUCK,
}
