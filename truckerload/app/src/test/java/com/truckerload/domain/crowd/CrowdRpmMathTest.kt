package com.truckerload.domain.crowd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrowdRpmMathTest {

    @Test
    fun median_oddAndEven() {
        assertEquals(2.0, CrowdRpmMath.median(listOf(3.0, 1.0, 2.0))!!, 0.001)
        assertEquals(2.5, CrowdRpmMath.median(listOf(1.0, 4.0, 2.0, 3.0))!!, 0.001)
        assertNull(CrowdRpmMath.median(emptyList()))
    }

    @Test
    fun percentileRank_isShareAtOrBelow() {
        val samples = listOf(1.0, 2.0, 3.0, 4.0)
        assertEquals(25, CrowdRpmMath.percentileRank(1.0, samples))
        assertEquals(75, CrowdRpmMath.percentileRank(3.0, samples))
        assertEquals(100, CrowdRpmMath.percentileRank(4.0, samples))
        assertNull(CrowdRpmMath.percentileRank(2.0, emptyList()))
    }

    @Test
    fun build_usesSimilarLanesWhenEnoughSamples() {
        val now = 1_000L
        val crowd = listOf(
            report("1", "WA", "OR", 2.0, now),
            report("2", "WA", "OR", 2.2, now),
            report("3", "WA", "OR", 3.0, now),
            report("4", "TX", "OK", 9.0, now),
        )
        val snapshot = CrowdRpmMath.build(
            myRpm = 2.5,
            myLanes = setOf("WA" to "OR"),
            crowd = crowd,
            extraAnonymousRpms = listOf(8.0),
        )
        assertTrue(snapshot.usedSimilarLanes)
        assertEquals(3, snapshot.sampleCount)
        assertEquals(3, snapshot.similarLaneCount)
        assertEquals(2.2, snapshot.medianRpm!!, 0.001)
        assertEquals(66, snapshot.percentile)
    }

    @Test
    fun build_fallsBackToGlobalPlusExtrasWhenFewSimilar() {
        val now = 1_000L
        val crowd = listOf(
            report("1", "WA", "OR", 2.0, now),
            report("2", "TX", "OK", 3.0, now, CrowdRateSource.ME),
        )
        val snapshot = CrowdRpmMath.build(
            myRpm = 2.5,
            myLanes = setOf("WA" to "OR"),
            crowd = crowd,
            extraAnonymousRpms = listOf(4.0, 5.0),
        )
        assertFalse(snapshot.usedSimilarLanes)
        assertEquals(3, snapshot.sampleCount)
        assertEquals(1, snapshot.similarLaneCount)
        assertEquals(4.0, snapshot.medianRpm!!, 0.001)
    }

    @Test
    fun build_emptyCommunityStillKeepsMyRpm() {
        val snapshot = CrowdRpmMath.build(
            myRpm = 2.4,
            myLanes = emptySet(),
            crowd = emptyList(),
        )
        assertEquals(2.4, snapshot.myRpm, 0.001)
        assertFalse(snapshot.hasCommunity)
        assertNull(snapshot.medianRpm)
    }

    @Test
    fun competitiveGate_requiresTwoFriends() {
        assertFalse(CommunityCompetitive.showFriendRanking(0))
        assertFalse(CommunityCompetitive.showFriendRanking(1))
        assertTrue(CommunityCompetitive.showFriendRanking(2))
        assertTrue(CommunityCompetitive.showFriendRanking(5))
    }

    private fun report(
        id: String,
        from: String,
        to: String,
        rpm: Double,
        at: Long,
        source: CrowdRateSource = CrowdRateSource.NETWORK,
    ) = CrowdRateReport(
        id = id,
        fromState = from,
        toState = to,
        rpm = rpm,
        rate = rpm * 400.0,
        miles = 400.0,
        reportedAtMillis = at,
        source = source,
    )
}
