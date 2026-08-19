package com.truckerload.presentation.screens.map

import com.truckerload.domain.crowd.AnonymizedRpmSample
import com.truckerload.domain.crowd.CrowdLaneAggregate
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdRateSource
import com.truckerload.domain.crowd.CrowdStateSummary
import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.StateRating
import com.truckerload.presentation.components.USHeatLevel
import com.truckerload.presentation.components.USStateMetric
import com.truckerload.presentation.components.getUsStateCodes
import com.truckerload.utils.extractStateFromLocation
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

/**
 * Pure map math from the driver's own loads: lane reports → outbound heatmap + recent feed.
 *
 * Crowd RPM buckets are (fromState) when equipment filter is null (all types),
 * or (fromState, equipmentType) when a trailer filter is selected.
 */
object CrowdMapAggregator {

    val WEEK_MS: Long = TimeUnit.DAYS.toMillis(7)

    /** Below this many samples, a filtered (state, equipment) cell is NO_DATA. */
    const val MIN_SAMPLE_SIZE = 5

    fun reportsFromLoads(
        loads: List<Load>,
        nowMillis: Long = System.currentTimeMillis(),
        windowMs: Long = WEEK_MS,
    ): List<CrowdRateReport> {
        val cutoff = nowMillis - windowMs
        val known = getUsStateCodes()
        return loads.mapNotNull { load ->
            val from = extractStateFromLocation(load.pointA)?.takeIf { it in known } ?: return@mapNotNull null
            val to = extractStateFromLocation(load.pointB)?.takeIf { it in known } ?: return@mapNotNull null
            if (load.totalMiles <= 0.0 || load.totalRate <= 0.0) return@mapNotNull null
            val at = reportTimeMillis(load)
            if (at < cutoff || at > nowMillis + TimeUnit.HOURS.toMillis(1)) return@mapNotNull null
            CrowdRateReport(
                id = "me:${load.id}",
                fromState = from,
                toState = to,
                rpm = load.totalRate / load.totalMiles,
                rate = load.totalRate,
                miles = load.totalMiles,
                reportedAtMillis = at,
                source = CrowdRateSource.ME,
                equipmentType = load.equipmentType,
            )
        }
    }

    fun filterByEquipment(
        reports: List<CrowdRateReport>,
        equipmentType: EquipmentType?,
    ): List<CrowdRateReport> =
        if (equipmentType == null) reports else reports.filter { it.equipmentType == equipmentType }

    fun toAnonymizedSample(report: CrowdRateReport, week: Int, year: Int): AnonymizedRpmSample =
        AnonymizedRpmSample(
            rpm = report.rpm,
            miles = report.miles,
            fromState = report.fromState,
            toState = report.toState,
            week = week,
            year = year,
            equipmentType = report.equipmentType,
        )

    fun filterMeOnly(reports: List<CrowdRateReport>): List<CrowdRateReport> =
        reports.filter { it.source == CrowdRateSource.ME }

    fun heatmapFromOutbound(
        reports: List<CrowdRateReport>,
        minSampleSize: Int = 0,
    ): List<USStateMetric> {
        val known = getUsStateCodes()
        data class Agg(val revenue: Double, val trips: Int, val miles: Double) {
            val rpm: Double get() = if (miles > 0) revenue / miles else 0.0
            fun enough(): Boolean = minSampleSize <= 0 || trips == 0 || trips >= minSampleSize
        }
        val byState = reports.groupBy { it.fromState }.mapValues { (_, list) ->
            Agg(
                revenue = list.sumOf { it.rate },
                trips = list.size,
                miles = list.sumOf { it.miles },
            )
        }
        val ranked = byState.entries
            .filter { it.value.rpm > 0 && it.value.enough() }
            .sortedByDescending { it.value.rpm }
        val n = ranked.size
        val goodCount = if (n > 0) maxOf(1, (n * 0.33).toInt()) else 0
        val badCount = if (n > 1) maxOf(1, (n * 0.33).toInt()) else 0
        val rpmRank = ranked.mapIndexed { idx, (code, _) -> code to idx }.toMap()

        return known.map { code ->
            val q = byState[code] ?: Agg(0.0, 0, 0.0)
            val insufficient = q.trips > 0 && minSampleSize > 0 && q.trips < minSampleSize
            val rank = rpmRank[code] ?: n
            val level = when {
                q.trips == 0 || insufficient -> USHeatLevel.LOW
                rank < goodCount -> USHeatLevel.HIGH
                rank < n - badCount -> USHeatLevel.MEDIUM
                else -> USHeatLevel.LOW
            }
            val rating = when {
                q.trips == 0 || insufficient -> StateRating.NO_DATA
                rank < goodCount -> StateRating.GOOD
                rank >= n - badCount && badCount > 0 -> StateRating.BAD
                else -> StateRating.NEUTRAL
            }
            USStateMetric(
                code = code,
                revenue = if (insufficient) 0.0 else q.revenue,
                trips = q.trips,
                level = level,
                revenuePerMile = if (insufficient) 0.0 else q.rpm,
                avgMilesPerTrip = if (!insufficient && q.trips > 0) q.miles / q.trips else 0.0,
                rating = rating,
            )
        }.sortedByDescending { it.revenue }
    }

    fun stateSummary(
        reports: List<CrowdRateReport>,
        stateCode: String,
        recentLimit: Int = 12,
        minSampleSize: Int = 0,
    ): CrowdStateSummary {
        val outbound = reports.filter { it.fromState == stateCode }
        val miles = outbound.sumOf { it.miles }
        val revenue = outbound.sumOf { it.rate }
        val insufficient = outbound.isNotEmpty() && minSampleSize > 0 && outbound.size < minSampleSize
        return CrowdStateSummary(
            stateCode = stateCode,
            outboundTrips = outbound.size,
            avgOutboundRpm = if (!insufficient && miles > 0) revenue / miles else 0.0,
            totalRevenue = if (insufficient) 0.0 else revenue,
            totalMiles = if (insufficient) 0.0 else miles,
            recent = outbound.sortedByDescending { it.reportedAtMillis }.take(recentLimit),
            sampleInsufficient = insufficient,
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

    private fun reportTimeMillis(load: Load): Long {
        if (load.parsedAt > 0L) return load.parsedAt
        if (load.updatedAt > 0L) return load.updatedAt
        return try {
            LocalDate.parse(load.date)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            0L
        }
    }
}
