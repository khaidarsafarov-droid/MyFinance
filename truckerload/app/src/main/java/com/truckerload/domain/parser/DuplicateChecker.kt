package com.truckerload.domain.parser

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load

sealed class DuplicateResult {
    data class Found(val load: Load) : DuplicateResult()
    data class Suspicious(val load: Load, val reason: String) : DuplicateResult()
    data object NotFound : DuplicateResult()
}

class DuplicateChecker(
    private val loadRepository: LoadRepository,
) {
    suspend fun checkDuplicate(parsedLoad: Load): DuplicateResult {
        val byTripId = loadRepository.getByTripId(parsedLoad.tripId)
        if (byTripId != null) {
            return DuplicateResult.Found(byTripId)
        }

        val metrics = parsedLoad
        val byRouteAndDate = loadRepository.getByRouteAndDate(
            origin = metrics.firstPuCityState.ifBlank { metrics.pointA },
            destination = metrics.lastDelCityState.ifBlank { metrics.pointB },
            date = metrics.date,
        )
        if (byRouteAndDate != null) {
            return DuplicateResult.Suspicious(
                load = byRouteAndDate,
                reason = "route+date",
            )
        }

        val byStops = loadRepository.getByStops(
            stops = parsedLoad.stops,
            date = parsedLoad.date,
        )
        if (byStops != null) {
            return DuplicateResult.Suspicious(
                load = byStops,
                reason = "stops",
            )
        }

        return DuplicateResult.NotFound
    }
}
