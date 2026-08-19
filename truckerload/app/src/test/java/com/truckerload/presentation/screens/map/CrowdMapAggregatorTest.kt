package com.truckerload.presentation.screens.map

import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdRateSource
import com.truckerload.domain.model.EquipmentType
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

    @Test
    fun reportsFromLoads_keepsMonthWindow() {
        val load = sampleLoad(
            id = "month",
            pointA = "Dallas, TX",
            pointB = "Austin, TX",
            rate = 800.0,
            miles = 200.0,
            parsedAt = now - TimeUnit.DAYS.toMillis(20),
        )
        assertEquals(
            1,
            CrowdMapAggregator.reportsFromLoads(
                listOf(load),
                nowMillis = now,
                windowMs = MapPeriod.MONTH.windowMs,
            ).size,
        )
        assertTrue(
            CrowdMapAggregator.reportsFromLoads(
                listOf(load),
                nowMillis = now,
                windowMs = MapPeriod.WEEK.windowMs,
            ).isEmpty(),
        )
    }

    @Test
    fun reportsFromLoads_yearIncludesOlderTrips() {
        val load = sampleLoad(
            id = "year",
            pointA = "Miami, FL",
            pointB = "Orlando, FL",
            rate = 900.0,
            miles = 250.0,
            parsedAt = now - TimeUnit.DAYS.toMillis(200),
        )
        assertEquals(
            1,
            CrowdMapAggregator.reportsFromLoads(
                listOf(load),
                nowMillis = now,
                windowMs = MapPeriod.YEAR.windowMs,
            ).size,
        )
        assertTrue(
            CrowdMapAggregator.reportsFromLoads(
                listOf(load),
                nowMillis = now,
                windowMs = MapPeriod.MONTH.windowMs,
            ).isEmpty(),
        )
    }

    @Test
    fun reportsFromLoads_copiesEquipmentType() {
        val load = sampleLoad(
            id = "eq",
            pointA = "Seattle, WA",
            pointB = "Portland, OR",
            rate = 1200.0,
            miles = 400.0,
            parsedAt = now - TimeUnit.DAYS.toMillis(1),
            equipmentType = EquipmentType.REEFER,
        )
        val reports = CrowdMapAggregator.reportsFromLoads(listOf(load), nowMillis = now)
        assertEquals(EquipmentType.REEFER, reports.single().equipmentType)
        val sample = CrowdMapAggregator.toAnonymizedSample(reports.single(), week = 31, year = 2026)
        assertEquals("WA", sample.fromState)
        assertEquals(EquipmentType.REEFER, sample.equipmentType)
        assertEquals(3.0, sample.rpm, 0.01)
    }

    @Test
    fun filterByEquipment_keepsMatchingTrailer() {
        val reports = listOf(
            CrowdRateReport("a", "WA", "OR", 2.0, 800.0, 400.0, now, CrowdRateSource.ME, equipmentType = EquipmentType.DRY_VAN),
            CrowdRateReport("b", "WA", "OR", 3.0, 1200.0, 400.0, now, CrowdRateSource.ME, equipmentType = EquipmentType.REEFER),
        )
        val reefer = CrowdMapAggregator.filterByEquipment(reports, EquipmentType.REEFER)
        assertEquals(1, reefer.size)
        assertEquals("b", reefer.single().id)
        assertEquals(2, CrowdMapAggregator.filterByEquipment(reports, null).size)
    }

    @Test
    fun heatmap_filteredBelowMinSample_isNoData() {
        val reports = List(3) { i ->
            CrowdRateReport("$i", "WA", "OR", 2.5, 1000.0, 400.0, now, CrowdRateSource.ME, equipmentType = EquipmentType.FLATBED)
        }
        val metrics = CrowdMapAggregator.heatmapFromOutbound(
            reports,
            minSampleSize = CrowdMapAggregator.MIN_SAMPLE_SIZE,
        )
        val wa = metrics.first { it.code == "WA" }
        assertEquals(3, wa.trips)
        assertEquals(StateRating.NO_DATA, wa.rating)
        assertEquals(0.0, wa.revenuePerMile, 0.0)
        val summary = CrowdMapAggregator.stateSummary(
            reports,
            "WA",
            minSampleSize = CrowdMapAggregator.MIN_SAMPLE_SIZE,
        )
        assertTrue(summary.sampleInsufficient)
        assertEquals(0.0, summary.avgOutboundRpm, 0.0)
    }

    @Test
    fun heatmap_filteredAtMinSample_showsAverage() {
        val reports = List(CrowdMapAggregator.MIN_SAMPLE_SIZE) { i ->
            CrowdRateReport("$i", "TX", "OK", 2.0, 800.0, 400.0, now, CrowdRateSource.ME, equipmentType = EquipmentType.DRY_VAN)
        }
        val metrics = CrowdMapAggregator.heatmapFromOutbound(
            reports,
            minSampleSize = CrowdMapAggregator.MIN_SAMPLE_SIZE,
        )
        val tx = metrics.first { it.code == "TX" }
        assertEquals(CrowdMapAggregator.MIN_SAMPLE_SIZE, tx.trips)
        assertTrue(tx.rating != StateRating.NO_DATA)
        assertEquals(2.0, tx.revenuePerMile, 0.01)
    }

    @Test
    fun heatmap_allFilter_aggregatesMixedEquipmentInState() {
        val reports = listOf(
            CrowdRateReport(
                "a", "WA", "OR", 2.0, 800.0, 400.0, now, CrowdRateSource.ME,
                equipmentType = EquipmentType.DRY_VAN,
            ),
            CrowdRateReport(
                "b", "WA", "OR", 4.0, 1600.0, 400.0, now, CrowdRateSource.ME,
                equipmentType = EquipmentType.REEFER,
            ),
        )
        val metrics = CrowdMapAggregator.heatmapFromOutbound(reports, minSampleSize = 0)
        val wa = metrics.first { it.code == "WA" }
        assertEquals(2, wa.trips)
        assertEquals(3.0, wa.revenuePerMile, 0.01)
        assertTrue(wa.rating != StateRating.NO_DATA)
    }

    private fun sampleLoad(
        id: String,
        pointA: String,
        pointB: String,
        rate: Double,
        miles: Double,
        parsedAt: Long,
        equipmentType: EquipmentType? = null,
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
        rawMessage = "SECRET MESSAGE",
        parsedAt = parsedAt,
        updatedAt = parsedAt,
        equipmentType = equipmentType,
    )
}
