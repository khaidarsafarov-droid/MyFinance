package com.truckerload.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartNotificationPlannerTest {

    @Test
    fun zeroLoadsEmptyWeek_notifiesBoth() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = false,
            dieselEntriesLastWeek = 0,
        )
        assertTrue(plan.notifyMissingPaycheck)
        assertTrue(plan.notifyMissingDiesel)
    }

    @Test
    fun presentPaycheckAndDiesel_noNotify() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = true,
            dieselEntriesLastWeek = 2,
        )
        assertFalse(plan.notifyMissingPaycheck)
        assertFalse(plan.notifyMissingDiesel)
    }
}
