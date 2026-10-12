package com.truckerload.domain.ux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `empty closed week keeps the default chips`() {
        assertEquals(listOf(5000.0, 6000.0, 7000.0), UxMotivation.suggestedWeeklyGoals(0.0))
    }

    @Test
    fun `closed week of 11000 suggests ten eleven and eleven five`() {
        assertEquals(listOf(10000.0, 11000.0, 11500.0), UxMotivation.suggestedWeeklyGoals(11000.0))
        assertEquals("10K", UxMotivation.formatSuggestedGoal(10000.0))
        assertEquals("11K", UxMotivation.formatSuggestedGoal(11000.0))
        assertEquals("$11,500", UxMotivation.formatSuggestedGoal(11500.0))
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
