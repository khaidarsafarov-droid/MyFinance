package com.truckerload.domain.social

import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialLoadStatsTest {

    @Test
    fun `overall score is weekly miles`() {
        val stats = WeeklyLoadStatsAgg(loadCount = 4, totalMiles = 1200.0, totalRevenue = 5000.0)
        assertEquals(1200.0, stats.leaderboardScore(LeaderboardCategory.OVERALL), 0.001)
    }

    @Test
    fun `loads score is load count`() {
        val stats = WeeklyLoadStatsAgg(loadCount = 7, totalMiles = 100.0, totalRevenue = 1.0)
        assertEquals(7.0, stats.leaderboardScore(LeaderboardCategory.LOADS), 0.001)
    }

    @Test
    fun `rpm score handles zero miles`() {
        val stats = WeeklyLoadStatsAgg(loadCount = 0, totalMiles = 0.0, totalRevenue = 100.0)
        assertEquals(0.0, stats.leaderboardScore(LeaderboardCategory.RPM), 0.001)
    }

    @Test
    fun `rpm score divides revenue by miles`() {
        val stats = WeeklyLoadStatsAgg(loadCount = 2, totalMiles = 800.0, totalRevenue = 2000.0)
        assertEquals(2.5, stats.leaderboardScore(LeaderboardCategory.RPM), 0.001)
    }
}
