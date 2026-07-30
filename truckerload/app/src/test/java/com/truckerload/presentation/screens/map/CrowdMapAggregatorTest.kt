package com.truckerload.presentation.screens.map

import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdRateSource
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.StateRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class CrowdMapAggregatorTest {

    private val now = 1_774_800_000_000L // fixed

    @Test
    fun reportsFromLoads_buildsLaneWithinWeek() {
        val load = sampleLoad(
            id = "1",
            pointA = "Seattle, WA",
            pointB = "Portland, OR",
            rate = 1200.0,
            miles = 400.0,
            parsedAt = now - TimeUnit.DAYS.toMillis(2),
        )
        val reports = CrowdMapAggregator.reportsFromLoads(listOf(load), nowMillis = now)
        assertEquals(1, reports.size)
        assertEquals("WA", reports[0].fromState)
        assertEquals("OR", reports[0].toState)
        assertEquals(3.0, reports[0].rpm, 0.01)
        assertEquals(CrowdRateSource.ME, reports[0].source)
    }

    @Test
    fun reportsFromLoads_skipsOlderThanWeek() {
        val load = sampleLoad(
            id = "old",
            pointA = "Dallas, TX",
            pointB = "Austin, TX",
            rate = 800.0,
            miles = 200.0,
            parsedAt = now - TimeUnit.DAYS.toMillis(10),
        )
        assertTrue(CrowdMapAggregator.reportsFromLoads(listOf(load), nowMillis = now).isEmpty())
    }

    @Test
    fun filterMeOnly_dropsNetwork() {
        val reports = listOf(
            CrowdRateReport("m", "WA", "OR", 2.5, 1000.0, 400.0, now, CrowdRateSource.ME),
            CrowdRateReport("n", "TX", "OK", 2.2, 900.0, 400.0, now, CrowdRateSource.NETWORK),
        )
        val me = CrowdMapAggregator.filterMeOnly(reports)
        assertEquals(1, me.size)
        assertEquals(CrowdRateSource.ME, me[0].source)
    }

    @Test
    fun stateSummary_listsRecentOutbound() {
        val reports = listOf(
            CrowdRateReport("1", "WA", "OR", 2.9, 1160.0, 400.0, now - TimeUnit.HOURS.toMillis(3), CrowdRateSource.ME),
            CrowdRateReport("2", "WA", "CA", 2.5, 1250.0, 500.0, now - TimeUnit.DAYS.toMillis(2), CrowdRateSource.ME),
            CrowdRateReport("3", "OR", "WA", 3.0, 900.0, 300.0, now, CrowdRateSource.ME),
        )
        val summary = CrowdMapAggregator.stateSummary(reports, "WA")
        assertEquals(2, summary.outboundTrips)
        assertEquals(2, summary.recent.size)
        assertEquals("OR", summary.recent[0].toState) // newest first
        assertTrue(summary.avgOutboundRpm > 0)
    }

    @Test
    fun heatmap_marksOutboundState() {
        val reports = listOf(
            CrowdRateReport("1", "WA", "OR", 3.0, 1200.0, 400.0, now, CrowdRateSource.ME),
        )
        val metrics = CrowdMapAggregator.heatmapFromOutbound(reports)
        val wa = metrics.first { it.code == "WA" }
        assertEquals(1, wa.trips)
        assertEquals(3.0, wa.revenuePerMile, 0.01)
        assertTrue(wa.rating != StateRating.NO_DATA)
        val or = metrics.first { it.code == "OR" }
        assertEquals(0, or.trips)
    }

    private fun sampleLoad(
        id: String,
        pointA: String,
        pointB: String,
        rate: Double,
        miles: Double,
        parsedAt: Long,
    ) = Load(
        id = id,
        tripId = "T-$id",
        date = "2026-07-28",
        totalRate = rate,
        totalMiles = miles,
        pointA = pointA,
        pointB = pointB,
        puCount = 1,
        delCount = 1,
        weekNumber = 31,
        year = 2026,
        rawMessage = "",
        parsedAt = parsedAt,
        updatedAt = parsedAt,
    )
}
