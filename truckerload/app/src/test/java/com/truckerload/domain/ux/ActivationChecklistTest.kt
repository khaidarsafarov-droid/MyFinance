package com.truckerload.domain.ux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivationChecklistTest {

    @Test
    fun `head start never starts at zero when only account is ready`() {
        val checklist = ActivationChecklistFactory.build(
            profileComplete = false,
            hasLoad = false,
            hasWeeklyGoal = false,
            hasDiesel = false,
        )
        assertEquals(1, checklist.completedCount)
        assertEquals(5, checklist.totalCount)
        assertEquals(0.2f, checklist.progressFraction, 0.001f)
        assertTrue(checklist.steps.first().second)
        assertFalse(checklist.allDone)
    }

    @Test
    fun `progress increases with each completed step`() {
        val half = ActivationChecklistFactory.build(
            profileComplete = true,
            hasLoad = true,
            hasWeeklyGoal = false,
            hasDiesel = false,
        )
        assertEquals(3, half.completedCount)
        assertEquals(0.6f, half.progressFraction, 0.001f)

        val done = ActivationChecklistFactory.build(
            profileComplete = true,
            hasLoad = true,
            hasWeeklyGoal = true,
            hasDiesel = true,
        )
        assertTrue(done.allDone)
        assertEquals(1f, done.progressFraction, 0.001f)
    }
}
