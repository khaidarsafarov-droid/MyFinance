package com.truckerload.domain.parser

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load

sealed class DuplicateResult {
    data class Found(val load: Load) : DuplicateResult()
    data class Suspicious(val load: Load, val reason: String) : DuplicateResult()
    data object NotFound : DuplicateResult()
}

/**
 * Live ingest duplicate gate.
 *
 * Route/date or stops/date matches alone are **not** enough to skip import — two
 * legitimate same-day loads on one lane (different trip ids / rates) must both land.
 * Aligns with [DuplicateAuditUseCase]: skip only when loads are effectively identical.
 */
class DuplicateChecker(
    private val loadRepository: LoadRepository,
) {
    suspend fun checkDuplicate(parsedLoad: Load): DuplicateResult {
        val byTripId = loadRepository.getByTripId(parsedLoad.tripId)
        if (byTripId != null) {
            return DuplicateResult.Found(byTripId)
        }

        val byRouteAndDate = loadRepository.getByRouteAndDate(
            origin = parsedLoad.firstPuCityState.ifBlank { parsedLoad.pointA },
            destination = parsedLoad.lastDelCityState.ifBlank { parsedLoad.pointB },
            date = parsedLoad.date,
        )
        if (byRouteAndDate != null && isLikelySameLoad(byRouteAndDate, parsedLoad)) {
            return DuplicateResult.Suspicious(
                load = byRouteAndDate,
                reason = "route+date",
            )
        }

        val byStops = loadRepository.getByStops(
            stops = parsedLoad.stops,
            date = parsedLoad.date,
        )
        if (byStops != null && isLikelySameLoad(byStops, parsedLoad)) {
            return DuplicateResult.Suspicious(
                load = byStops,
                reason = "stops",
            )
        }

        return DuplicateResult.NotFound
    }

    /**
     * True when [existing] and [incoming] should be treated as the same load for ingest.
     * Different rates on the same route/date → allow insert (NotFound path).
     */
    internal fun isLikelySameLoad(existing: Load, incoming: Load): Boolean {
        if (existing.tripId.equals(incoming.tripId, ignoreCase = true)) return true
        val comparison = compareLoads(old = existing, new = incoming)
        if (comparison.isIdentical()) return true
        return existing.date == incoming.date &&
            comparison.stopsHashMatch &&
            comparison.totalRateMatch
    }
}
