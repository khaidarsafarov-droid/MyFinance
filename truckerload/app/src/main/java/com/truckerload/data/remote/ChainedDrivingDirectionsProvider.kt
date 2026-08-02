package com.truckerload.data.remote

import com.truckerload.domain.friends.DrivingDirectionsProvider
import com.truckerload.domain.friends.DrivingRouteOptions
import com.truckerload.domain.friends.DrivingRouteResult
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.VehicleRoutingMode

/**
 * Tries providers in order until one returns a road polyline.
 * Order: OpenRouteService (truck HGV) → Google Directions → OSRM public.
 */
class ChainedDrivingDirectionsProvider(
    private val providers: List<DrivingDirectionsProvider>,
) : DrivingDirectionsProvider {
    override val providerLabel: String = "Chained"

    override fun isConfigured(): Boolean = providers.any { it.isConfigured() }

    override suspend fun fetchDrivingRoute(
        origin: LatLngPoint,
        destination: LatLngPoint,
        options: DrivingRouteOptions,
    ): Result<DrivingRouteResult> {
        val ordered = orderProviders(options)
        var lastError: Throwable? = null
        for (provider in ordered) {
            if (!provider.isConfigured()) continue
            val result = provider.fetchDrivingRoute(origin, destination, options)
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
        }
        return Result.failure(
            lastError ?: IllegalStateException("No directions provider configured"),
        )
    }

    private fun orderProviders(options: DrivingRouteOptions): List<DrivingDirectionsProvider> {
        val ors = providers.filterIsInstance<OpenRouteServiceDirectionsClient>().firstOrNull()
        val google = providers.filterIsInstance<GoogleDirectionsClient>().firstOrNull()
        val osrm = providers.filterIsInstance<OsrmDirectionsClient>().firstOrNull()
        val rest = providers.filter { it !is OpenRouteServiceDirectionsClient &&
            it !is GoogleDirectionsClient && it !is OsrmDirectionsClient }
        return when (options.vehicleMode) {
            VehicleRoutingMode.TRUCK -> listOfNotNull(ors, google, osrm) + rest
            VehicleRoutingMode.CAR -> listOfNotNull(google, ors, osrm) + rest
        }
    }
}

object DrivingDirectionsProviders {
    fun createDefault(): ChainedDrivingDirectionsProvider = ChainedDrivingDirectionsProvider(
        listOf(
            OpenRouteServiceDirectionsClient(),
            GoogleDirectionsClient(),
            OsrmDirectionsClient(),
        ),
    )
}
