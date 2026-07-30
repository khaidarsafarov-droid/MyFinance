package com.truckerload.sync

import org.junit.Assert.assertEquals
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
        assertTrue(plan.hasAny)
    }

    @Test
    fun presentPaycheckAndDiesel_noNotify() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = true,
            dieselEntriesLastWeek = 2,
        )
        assertFalse(plan.notifyMissingPaycheck)
        assertFalse(plan.notifyMissingDiesel)
        assertTrue(plan.maintenanceDueTitles.isEmpty())
        assertFalse(plan.hasAny)
    }

    @Test
    fun alreadyNotifiedMissingWeek_suppressesPaycheckAndDiesel() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = false,
            dieselEntriesLastWeek = 0,
            alreadyNotifiedMissingWeek = true,
        )
        assertFalse(plan.notifyMissingPaycheck)
        assertFalse(plan.notifyMissingDiesel)
    }

    @Test
    fun maintenanceDueTitles_passThroughAndSummarize() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = true,
            dieselEntriesLastWeek = 1,
            maintenanceDueTitles = listOf("Oil change", "Tires"),
        )
        assertEquals(listOf("Oil change", "Tires"), plan.maintenanceDueTitles)
        assertEquals("Oil change, Tires", plan.maintenanceSummaryBody())
        assertTrue(plan.hasAny)
    }
}
