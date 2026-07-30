package com.truckerload.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupAlarmPlannerTest {

    private val pickup = 1_700_000_000_000L

    @Test
    fun shouldPrompt_futurePickup_true() {
        assertTrue(PickupAlarmPlanner.shouldPromptForAlarm(pickup, pickup - 60_000L))
    }

    @Test
    fun shouldPrompt_pastPickup_false() {
        assertFalse(PickupAlarmPlanner.shouldPromptForAlarm(pickup, pickup + 1L))
    }

    @Test
    fun shouldPrompt_nullPickup_false() {
        assertFalse(PickupAlarmPlanner.shouldPromptForAlarm(null, pickup))
    }

    @Test
    fun alarmAtOffset_twoHoursBefore_future() {
        val now = pickup - PickupAlarmPlanner.OFFSET_TWO_HOURS_MS - 1_000L
        val alarm = PickupAlarmPlanner.alarmAtOffset(
            pickup,
            PickupAlarmPlanner.OFFSET_TWO_HOURS_MS,
            now,
        )
        assertEquals(pickup - PickupAlarmPlanner.OFFSET_TWO_HOURS_MS, alarm)
    }

    @Test
    fun alarmAtOffset_twoHoursBefore_past_returnsNull() {
        val now = pickup - PickupAlarmPlanner.OFFSET_TWO_HOURS_MS + 1_000L
        val alarm = PickupAlarmPlanner.alarmAtOffset(
            pickup,
            PickupAlarmPlanner.OFFSET_TWO_HOURS_MS,
            now,
        )
        assertNull(alarm)
    }

    @Test
    fun alarmAtCustom_future() {
        val custom = pickup - 30 * 60_000L
        assertEquals(custom, PickupAlarmPlanner.alarmAtCustom(custom, custom - 1_000L))
    }

    @Test
    fun alarmAtCustom_past_returnsNull() {
        val custom = pickup - 30 * 60_000L
        assertNull(PickupAlarmPlanner.alarmAtCustom(custom, custom + 1_000L))
    }
}
