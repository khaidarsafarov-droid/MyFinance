package com.truckerload.domain.friends

/**
 * Vehicle profile for driving directions requests.
 * Truck is the default for this app; car uses standard driving profiles.
 */
enum class VehicleRoutingMode {
    CAR,
    TRUCK,
}

/**
 * Truck-specific routing constraints.
 * Full HAZMAT / tunnel / axle enforcement requires a commercial truck API
 * (TomTom Truck, HERE, OpenRouteService HGV with API key).
 */
data class TruckVehicleSpec(
    val heightMeters: Double = 4.0,
    val widthMeters: Double = 2.6,
    val lengthMeters: Double = 16.0,
    val weightTons: Double = 36.0,
    val axleLoadTons: Double = 10.0,
    val hazardousMaterial: Boolean = false,
    val tunnelCategory: String? = null,
)

data class DrivingRouteOptions(
    val vehicleMode: VehicleRoutingMode = VehicleRoutingMode.TRUCK,
    val truckSpec: TruckVehicleSpec = TruckVehicleSpec(),
    /** When true, prefer providers that support HGV restrictions. */
    val avoidFerries: Boolean = true,
)
