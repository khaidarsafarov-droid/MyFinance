package com.truckerload.presentation.screens.map

import com.truckerload.domain.crowd.AnonymizedRpmSample
import com.truckerload.domain.crowd.CrowdLaneAggregate
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdRateSource
import com.truckerload.domain.crowd.CrowdRpmMapper
import com.truckerload.domain.crowd.CrowdStateSummary
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.StateRating
import com.truckerload.presentation.components.USHeatLevel
import com.truckerload.presentation.components.USStateMetric
import com.truckerload.presentation.components.getUsStateCodes
import java.util.concurrent.TimeUnit

/**
 * Pure **local** map math from the driver's own loads.
 *
 * Heatmap rows are built from [CrowdRpmMapper] (rpm + miles + 2-letter states).
 * They never use `Load.id` / `tripId` as report ids. `CrowdRateReport.rate` is
 * reconstructed as `rpm * miles` for local coloring only — it is not a Crowd RPM
 * share field. See `docs/CROWD_RPM_PRIVACY.md`.
 *
 * Heatmap buckets are (fromState) across all trailer types on the driver's loads.
 */
object CrowdMapAggregator {

    val WEEK_MS: Long = CrowdRpmMapper.WEEK_MS

    fun reportsFromLoads(
        loads: List<Load>,
        nowMillis: Long = System.currentTimeMillis(),
        windowMs: Long = WEEK_MS,
    ): List<CrowdRateReport> {
        val cutoff = nowMillis - windowMs
        val skew = nowMillis + TimeUnit.HOURS.toMillis(1)
        val known = getUsStateCodes()
        return loads.mapIndexedNotNull { index, load ->
            val at = CrowdRpmMapper.eventTimeMillis(load)
            if (at < cutoff || at > skew) return@mapIndexedNotNull null
            val sample = CrowdRpmMapper.fromLoad(load) ?: return@mapIndexedNotNull null
            val from = CrowdRpmMapper.originState(load)?.takeIf { it in known }
                ?: return@mapIndexedNotNull null
            val to = CrowdRpmMapper.destState(load)?.takeIf { it in known }
                ?: return@mapIndexedNotNull null
            CrowdRateReport(
                id = "anon:$index",
                fromState = from,
                toState = to,
                rpm = sample.rpm,
                rate = sample.rpm * sample.miles,
                miles = sample.miles,
                reportedAtMillis = at,
                source = CrowdRateSource.ME,
                equipmentType = load.equipmentType,
            )
        }
    }

    fun toAnonymizedSample(report: CrowdRateReport, week: Int, year: Int): AnonymizedRpmSample =
        AnonymizedRpmSample(
            rpm = report.rpm,
            miles = report.miles,
            region = CrowdRpmMapper.regionOf(report.fromState, report.toState),
            weekNumber = week,
            fromState = report.fromState,
            toState = report.toState,
            week = week,
            year = year,
            equipmentType = report.equipmentType,
        )

    fun filterMeOnly(reports: List<CrowdRateReport>): List<CrowdRateReport> =
        reports.filter { it.source == CrowdRateSource.ME }

    fun heatmapFromOutbound(reports: List<CrowdRateReport>): List<USStateMetric> {
        val known = getUsStateCodes()
        data class Agg(val revenue: Double, val trips: Int, val miles: Double) {
            val rpm: Double get() = if (miles > 0) revenue / miles else 0.0
        }
        val byState = reports.groupBy { it.fromState }.mapValues { (_, list) ->
            Agg(
                revenue = list.sumOf { it.rate },
                trips = list.size,
                miles = list.sumOf { it.miles },
            )
        }
        val ranked = byState.entries
            .filter { it.value.rpm > 0 }
            .sortedByDescending { it.value.rpm }
        val n = ranked.size
        val goodCount = if (n > 0) maxOf(1, (n * 0.33).toInt()) else 0
        val badCount = if (n > 1) maxOf(1, (n * 0.33).toInt()) else 0
        val rpmRank = ranked.mapIndexed { idx, (code, _) -> code to idx }.toMap()

        return known.map { code ->
            val q = byState[code] ?: Agg(0.0, 0, 0.0)
            val rank = rpmRank[code] ?: n
            val level = when {
                q.trips == 0 -> USHeatLevel.LOW
                rank < goodCount -> USHeatLevel.HIGH
                rank < n - badCount -> USHeatLevel.MEDIUM
                else -> USHeatLevel.LOW
            }
            val rating = when {
                q.trips == 0 -> StateRating.NO_DATA
                rank < goodCount -> StateRating.GOOD
                rank >= n - badCount && badCount > 0 -> StateRating.BAD
                else -> StateRating.NEUTRAL
            }
            USStateMetric(
                code = code,
                revenue = q.revenue,
                trips = q.trips,
                level = level,
                revenuePerMile = q.rpm,
                avgMilesPerTrip = if (q.trips > 0) q.miles / q.trips else 0.0,
                rating = rating,
            )
        }.sortedByDescending { it.revenue }
    }

    fun stateSummary(
        reports: List<CrowdRateReport>,
        stateCode: String,
        recentLimit: Int = 12,
    ): CrowdStateSummary {
        val outbound = reports.filter { it.fromState == stateCode }
        val miles = outbound.sumOf { it.miles }
        val revenue = outbound.sumOf { it.rate }
        return CrowdStateSummary(
            stateCode = stateCode,
            outboundTrips = outbound.size,
            avgOutboundRpm = if (miles > 0) revenue / miles else 0.0,
            totalRevenue = revenue,
            totalMiles = miles,
            recent = outbound.sortedByDescending { it.reportedAtMillis }.take(recentLimit),
        )
    }

    fun topLanes(reports: List<CrowdRateReport>, limit: Int = 8): List<CrowdLaneAggregate> =
        reports.groupBy { it.fromState to it.toState }
            .map { (lane, list) ->
                val miles = list.sumOf { it.miles }
                val revenue = list.sumOf { it.rate }
                CrowdLaneAggregate(
                    fromState = lane.first,
                    toState = lane.second,
                    tripCount = list.size,
                    avgRpm = if (miles > 0) revenue / miles else 0.0,
                    totalRevenue = revenue,
                    totalMiles = miles,
                )
            }
            .sortedByDescending { it.tripCount }
            .take(limit)
}
