package com.truckerload.presentation.screens.social

import com.truckerload.domain.social.LeaderboardCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardScoreFormatTest {

    @Test
    fun overall_includesMilesUnit() {
        val text = formatLeaderboardScore(LeaderboardCategory.OVERALL, 1425.0)
        assertTrue(text.contains("1,425"))
        assertTrue(text.endsWith(" mi"))
    }

    @Test
    fun revenue_isCurrency() {
        val text = formatLeaderboardScore(LeaderboardCategory.REVENUE, 2500.0)
        assertTrue(text.startsWith("$"))
        assertTrue(text.contains("2,500"))
    }

    @Test
    fun rpm_isShortDollars() {
        assertEquals("$2.50", formatLeaderboardScore(LeaderboardCategory.RPM, 2.5))
    }

    @Test
    fun loads_isPlainCount() {
        assertEquals("12", formatLeaderboardScore(LeaderboardCategory.LOADS, 12.0))
    }
}
