package com.truckerload.domain.social

import com.truckerload.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialLoadStatsTest {

    @Test
    fun `overall score is weekly miles`() {
        val stats = stats(loadCount = 4, miles = 1200.0, revenueDollars = 5000.0)
        assertEquals(1200.0, stats.leaderboardScore(LeaderboardCategory.OVERALL), 0.001)
    }

    @Test
    fun `loads score is load count`() {
        val stats = stats(loadCount = 7, miles = 100.0, revenueDollars = 1.0)
        assertEquals(7.0, stats.leaderboardScore(LeaderboardCategory.LOADS), 0.001)
    }

    @Test
    fun `rpm score handles zero miles`() {
        val stats = stats(loadCount = 0, miles = 0.0, revenueDollars = 100.0)
        assertEquals(0.0, stats.leaderboardScore(LeaderboardCategory.RPM), 0.001)
    }

    @Test
    fun `rpm score divides revenue by miles`() {
        val stats = stats(loadCount = 2, miles = 800.0, revenueDollars = 2000.0)
        assertEquals(2.5, stats.leaderboardScore(LeaderboardCategory.RPM), 0.001)
    }

    private fun stats(loadCount: Int, miles: Double, revenueDollars: Double) = LeaderboardStats(
        loadCount = loadCount,
        totalMiles = miles,
        totalRevenueCents = Money.fromDollars(revenueDollars).cents,
    )
}
