package com.truckerload.domain.ux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UxMotivationTest {

    @Test
    fun `loss aversion prefers behind goal over other signals`() {
        val signal = UxMotivation.lossAversion(
            goalConfigured = true,
            targetAmount = 5000.0,
            currentGross = 1000.0,
            remainingAmount = 4000.0,
            daysRemaining = 3,
            isBehindPace = true,
            loadCountThisWeek = 1,
        )
        assertEquals(LossAversionKind.GOAL_BEHIND, signal.kind)
        assertEquals(4000.0, signal.remainingAmount, 0.01)
    }

    @Test
    fun `loss aversion prompts goal when earning without target`() {
        val signal = UxMotivation.lossAversion(
            goalConfigured = false,
            targetAmount = 0.0,
            currentGross = 2200.0,
            remainingAmount = 0.0,
            daysRemaining = 4,
            isBehindPace = false,
            loadCountThisWeek = 2,
        )
        assertEquals(LossAversionKind.GOAL_UNSET_WITH_EARNINGS, signal.kind)
    }

    @Test
    fun `suggested goals scale with recent gross`() {
        val fresh = UxMotivation.suggestedWeeklyGoals(0.0)
        assertEquals(listOf(3000.0, 5000.0, 7000.0), fresh)

        val mid = UxMotivation.suggestedWeeklyGoals(5200.0)
        assertTrue(mid.contains(5000.0))
        assertEquals(3, mid.size)
    }

    @Test
    fun `diesel contrast anchors on gross`() {
        val contrast = UxMotivation.dieselContrast(gross = 4000.0, diesel = 800.0)
        requireNotNull(contrast)
        assertEquals(4000.0, contrast.anchorAmount, 0.01)
        assertEquals(20.0, contrast.comparedPercentOfAnchor, 0.01)
        assertNull(UxMotivation.dieselContrast(gross = 0.0, diesel = 100.0))
    }
}
