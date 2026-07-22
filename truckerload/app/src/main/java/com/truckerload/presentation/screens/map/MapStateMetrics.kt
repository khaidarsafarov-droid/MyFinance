package com.truckerload.presentation.screens.map

import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.StateRating
import com.truckerload.presentation.components.USHeatLevel
import com.truckerload.presentation.components.USStateMetric
import com.truckerload.presentation.components.getUsStateCodes
import com.truckerload.utils.extractStateFromLocation

/** Pure heatmap metrics from loads (empty list → all states NO_DATA / LOW). */
object MapStateMetrics {

    fun computeFromLoads(loads: List<Load>): List<USStateMetric> {
        val knownCodes = getUsStateCodes()
        val loadsWithState = loads
            .filter { it.pointB.isNotBlank() && it.totalMiles > 0 }
            .mapNotNull { load ->
                extractStateFromLocation(load.pointB)?.let { state ->
                    if (state in knownCodes) Triple(state, load.totalRate, load.totalMiles) else null
                }
            }
        data class StateAgg(val revenue: Double, val trips: Int, val avgMiles: Double, val rpm: Double)
        val byState = loadsWithState.groupBy { it.first }.mapValues { (_, list) ->
            val revenue = list.sumOf { it.second }
            val totalMiles = list.sumOf { it.third }
            val trips = list.size
            val avgMiles = if (trips > 0) totalMiles / trips else 0.0
            val rpm = if (totalMiles > 0) revenue / totalMiles else 0.0
            StateAgg(revenue, trips, avgMiles, rpm)
        }

        val statesWithRpm = byState.entries
            .filter { (_, q) -> q.rpm > 0 }
            .sortedByDescending { it.value.rpm }
        val n = statesWithRpm.size
        val goodCount = if (n > 0) maxOf(1, (n * 0.33).toInt()) else 0
        val badCount = if (n > 1) maxOf(1, (n * 0.33).toInt()) else 0
        val rpmRank = statesWithRpm.mapIndexed { idx, (code, _) -> code to idx }.toMap()

        val result = mutableListOf<USStateMetric>()
        for (code in knownCodes) {
            val q = byState[code] ?: StateAgg(0.0, 0, 0.0, 0.0)
            val (revenue, trips, avgMiles, rpm) = q
            val rank = rpmRank[code] ?: n
            val level = when {
                trips == 0 -> USHeatLevel.LOW
                rank < goodCount -> USHeatLevel.HIGH
                rank < n - badCount -> USHeatLevel.MEDIUM
                else -> USHeatLevel.LOW
            }
            val rating = when {
                trips == 0 -> StateRating.NO_DATA
                rank < goodCount -> StateRating.GOOD
                rank >= n - badCount && badCount > 0 -> StateRating.BAD
                else -> StateRating.NEUTRAL
            }
            result.add(
                USStateMetric(
                    code = code,
                    revenue = revenue,
                    trips = trips,
                    level = level,
                    revenuePerMile = rpm,
                    avgMilesPerTrip = avgMiles,
                    rating = rating,
                ),
            )
        }
        return result.sortedByDescending { it.revenue }
    }
}
