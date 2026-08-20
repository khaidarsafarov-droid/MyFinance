package com.truckerload.domain.crowd

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class CrowdRpmMapperTest {

    private val now = 1_774_800_000_000L

    @Test
    fun fromLoad_keepsOnlyRpmMilesRegionAndWeek() {
        val sample = CrowdRpmMapper.fromLoad(
            sampleLoad(
                id = "secret-id",
                tripId = "T-SECRET",
                pointA = "SWF2, Garner, NC",
                pointB = "Portland, OR",
                rate = 1200.0,
                miles = 400.0,
                parsedAt = now,
            ),
        )
        assertNotNull(sample)
        requireNotNull(sample)
        assertEquals(3.0, sample.rpm, 0.01)
        assertEquals(400.0, sample.miles, 0.01)
        assertEquals("NC-OR", sample.region)
        assertTrue(sample.weekNumber in 1..53)
        val dumped = sample.toString()
        assertFalse(dumped.contains("secret-id"))
        assertFalse(dumped.contains("T-SECRET"))
        assertFalse(dumped.contains("Garner"))
        assertFalse(dumped.contains("SWF2"))
        assertFalse(dumped.contains("Portland"))
        assertFalse(dumped.contains("raw"))
    }

    @Test
    fun fromLoad_sameStateRegionIsSingleCode() {
        val sample = CrowdRpmMapper.fromLoad(
            sampleLoad(
                pointA = "Dallas, TX",
                pointB = "Austin, TX",
                rate = 800.0,
                miles = 200.0,
                parsedAt = now,
            ),
        )
        assertEquals("TX", sample?.region)
    }

    @Test
    fun fromLoad_skipsZeroMiles() {
        assertNull(
            CrowdRpmMapper.fromLoad(
                sampleLoad(rate = 100.0, miles = 0.0, parsedAt = now),
            ),
        )
    }

    @Test
    fun samplesInWindow_dropsOldLoads() {
        val fresh = sampleLoad(id = "new", parsedAt = now - TimeUnit.DAYS.toMillis(2))
        val old = sampleLoad(id = "old", parsedAt = now - TimeUnit.DAYS.toMillis(10))
        val samples = CrowdRpmMapper.samplesInWindow(listOf(fresh, old), nowMillis = now)
        assertEquals(1, samples.size)
        assertFalse(samples[0].toString().contains("old"))
        assertFalse(samples[0].toString().contains("new"))
    }

    @Test
    fun shareGate_requiresOptIn() {
        val sample = AnonymizedRpmSample(rpm = 2.5, miles = 100.0, region = "WA", weekNumber = 12)
        assertNull(CrowdRpmShareGate.payloadOrNull(optIn = false, samples = listOf(sample)))
        assertEquals(
            listOf(sample),
            CrowdRpmShareGate.payloadOrNull(optIn = true, samples = listOf(sample)),
        )
    }

    private fun sampleLoad(
        id: String = "id",
        tripId: String = "T-$id",
        pointA: String = "Seattle, WA",
        pointB: String = "Portland, OR",
        rate: Double = 1200.0,
        miles: Double = 400.0,
        parsedAt: Long,
    ) = Load(
        id = id,
        tripId = tripId,
        date = "2026-07-28",
        totalRate = rate,
        totalMiles = miles,
        pointA = pointA,
        pointB = pointB,
        puCount = 1,
        delCount = 1,
        weekNumber = 31,
        year = 2026,
        rawMessage = "RAW secret message",
        parsedAt = parsedAt,
        updatedAt = parsedAt,
    )
}
