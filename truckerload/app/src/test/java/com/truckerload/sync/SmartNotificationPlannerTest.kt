package com.truckerload.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartNotificationPlannerTest {

    @Test
    fun zeroLoadsEmptyWeek_doesNotNagPaycheckOrDiesel() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = false,
            dieselEntriesLastWeek = 0,
            hadLoadsLastWeek = false,
        )
        assertFalse(plan.notifyMissingPaycheck)
        assertFalse(plan.notifyMissingDiesel)
    }

    @Test
    fun loadsWithoutPaycheckOrDiesel_notifiesBoth() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = false,
            dieselEntriesLastWeek = 0,
            hadLoadsLastWeek = true,
        )
        assertTrue(plan.notifyMissingPaycheck)
        assertTrue(plan.notifyMissingDiesel)
    }

    @Test
    fun presentPaycheckAndDiesel_noNotify() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = true,
            dieselEntriesLastWeek = 2,
            hadLoadsLastWeek = true,
        )
        assertFalse(plan.notifyMissingPaycheck)
        assertFalse(plan.notifyMissingDiesel)
        assertTrue(plan.maintenanceDueTitles.isEmpty())
    }

    @Test
    fun maintenanceDueTitles_passThroughEvenWithoutLoads() {
        val plan = SmartNotificationPlanner.plan(
            hasPaycheckForLastWeek = true,
            dieselEntriesLastWeek = 1,
            maintenanceDueTitles = listOf("Oil change"),
            hadLoadsLastWeek = false,
        )
        assertEquals(listOf("Oil change"), plan.maintenanceDueTitles)
        assertFalse(plan.notifyMissingPaycheck)
        assertFalse(plan.notifyMissingDiesel)
    }
}
