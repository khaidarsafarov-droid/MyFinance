package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceReminderPlannerTest {

    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 10, 29, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun dateReminder_schedulesEightAm_beforeItIsDue() {
        val task = dateTask(due = "2026-11-01")
        val evaluation = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = emptyMap(),
        )
        val alarm = evaluation.alarms.single()
        assertEquals(task.id, alarm.alarmId)
        assertEquals(
            MaintenanceReminderPlanner.atLocalTime(LocalDate.of(2026, 11, 1), zone),
            alarm.triggerAtMillis,
        )
        assertFalse(evaluation.plans.single().notify)
    }

    @Test
    fun overdueDate_notifiesNow_andOneShotDoesNotScheduleAnother() {
        val task = dateTask(due = "2026-10-28")
        val evaluation = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = emptyMap(),
        )
        val plan = evaluation.plans.single()
        assertTrue(plan.notify)
        assertNull(plan.alarm)
        assertEquals("date:2026-10-28", plan.state.lastFiredCycleKey)
        assertTrue(evaluation.alarms.isEmpty())
    }

    @Test
    fun recurringDate_afterFire_schedulesTheNextOccurrence() {
        val task = dateTask(due = "2026-10-28", repeatDays = 30)
        val evaluation = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = emptyMap(),
        )
        val plan = evaluation.plans.single()
        assertTrue(plan.notify)
        val next = MaintenanceReminderPlanner.nextDueDate(LocalDate.of(2026, 10, 28), 30, today)
        assertEquals(LocalDate.of(2026, 11, 27), next)
        assertEquals(
            MaintenanceReminderPlanner.atLocalTime(next, zone),
            plan.alarm?.triggerAtMillis,
        )
        val again = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = mapOf(task.id to plan.state),
        )
        assertFalse(again.plans.single().notify)
        assertEquals(plan.alarm?.triggerAtMillis, again.plans.single().alarm?.triggerAtMillis)
    }

    @Test
    fun doneOnRecurringDate_advancesDue_andDoesNotDoubleAdvance() {
        val task = dateTask(due = "2026-10-28", repeatDays = 30)
        val fired = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = emptyMap(),
        ).plans.single()
        val (advanced, state) = MaintenanceReminderPlanner.afterDone(
            task = task,
            state = fired.state,
            today = today,
            estimatedOdometer = null,
            nowMillis = now,
        )
        assertEquals("2026-11-27", advanced.dueDate)
        assertFalse(advanced.isCompleted)
        val next = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(advanced),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = mapOf(advanced.id to state),
        )
        assertFalse(next.plans.single().notify)
        assertEquals(
            MaintenanceReminderPlanner.atLocalTime(LocalDate.of(2026, 11, 27), zone),
            next.alarms.single().triggerAtMillis,
        )
    }

    @Test
    fun doneOnOneShot_completesAndCancels() {
        val task = dateTask(due = "2026-11-01")
        val (completed, _) = MaintenanceReminderPlanner.afterDone(
            task = task,
            state = MaintenanceReminderPlanner.FireState(),
            today = today,
            estimatedOdometer = null,
            nowMillis = now,
        )
        assertTrue(completed.isCompleted)
        val evaluation = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(completed),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = emptyMap(),
        )
        assertTrue(evaluation.alarms.isEmpty())
        assertTrue(evaluation.plans.isEmpty())
    }

    @Test
    fun snooze_overridesTheCycle_untilItIsDue() {
        val task = dateTask(due = "2026-11-01")
        val snoozedUntil = now + MaintenanceReminderPlanner.SNOOZE_MILLIS
        val state = MaintenanceReminderPlanner.afterSnooze(
            MaintenanceReminderPlanner.FireState(fingerprint = MaintenanceReminderPlanner.fingerprint(task)),
            now,
            zone,
            MaintenanceReminderPlanner.QuietWindow(),
        )
        assertEquals(snoozedUntil, state.snoozeUntilMillis)
        val waiting = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = mapOf(task.id to state),
        )
        assertEquals(snoozedUntil, waiting.alarms.single().triggerAtMillis)
        assertTrue(waiting.alarms.single().replay)
        assertFalse(waiting.plans.single().notify)
        val due = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = snoozedUntil,
            zone = zone,
            milesDue = emptyMap(),
            states = mapOf(task.id to state),
        )
        assertTrue(due.plans.single().notify)
    }

    @Test
    fun milesDue_notifiesThenNagsNextMorning_untilDoneRollsTheInterval() {
        val task = milesTask()
        val first = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = now,
            zone = zone,
            milesDue = mapOf(task.id to true),
            states = emptyMap(),
        )
        val plan = first.plans.single()
        assertTrue(plan.notify)
        assertNotNull(plan.alarm)
        assertTrue(plan.alarm!!.triggerAtMillis > now)
        assertTrue(first.alarms.any { it.alarmId == MaintenanceReminderPlanner.ANCHOR_ID })
        val nag = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = plan.alarm.triggerAtMillis,
            zone = zone,
            milesDue = mapOf(task.id to true),
            states = mapOf(task.id to plan.state),
        )
        assertTrue(nag.plans.single().notify)
        val (rolled, reset) = MaintenanceReminderPlanner.afterDone(
            task = task,
            state = plan.state,
            today = today,
            estimatedOdometer = 110_000.0,
            nowMillis = now,
        )
        assertEquals(today.toString(), rolled.startDate)
        assertEquals(110_000.0, rolled.odometerAtStart)
        assertFalse(rolled.isCompleted)
        val after = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(rolled),
            nowMillis = now,
            zone = zone,
            milesDue = mapOf(rolled.id to false),
            states = mapOf(rolled.id to reset),
        )
        assertFalse(after.plans.single().notify)
        assertNull(after.plans.single().alarm)
    }

    @Test
    fun edit_clearsFiredState_soTheNewDueCanRing() {
        val original = dateTask(due = "2026-10-28")
        val fired = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(original),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = emptyMap(),
        ).plans.single().state
        val edited = original.copy(dueDate = "2026-12-01")
        val state = MaintenanceReminderPlanner.effectiveState(edited, fired)
        assertNull(state.lastFiredCycleKey)
        val evaluation = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(edited),
            nowMillis = now,
            zone = zone,
            milesDue = emptyMap(),
            states = mapOf(edited.id to state),
        )
        assertFalse(evaluation.plans.single().notify)
        assertEquals(
            MaintenanceReminderPlanner.atLocalTime(LocalDate.of(2026, 12, 1), zone),
            evaluation.alarms.single().triggerAtMillis,
        )
    }

    @Test
    fun reconcile_cancelsDeletedAndRearmsChangedTriggers() {
        val kept = occurrence(id = 1, trigger = 100, cycle = "a")
        val changed = occurrence(id = 2, trigger = 200, cycle = "b")
        val removed = occurrence(id = 3, trigger = 300, cycle = "c")
        val desiredChanged = changed.copy(triggerAtMillis = 250)
        val result = MaintenanceReminderPlanner.reconcile(
            armed = listOf(kept, changed, removed),
            desired = listOf(kept, desiredChanged),
        )
        assertEquals(setOf(2L, 3L), result.cancelIds)
        assertEquals(listOf(desiredChanged), result.arm)
    }

    @Test
    fun quietHours_delayAnOverdueRingUntilTheWindowEnds() {
        val task = dateTask(due = "2026-10-28")
        val night = ZonedDateTime.of(2026, 10, 29, 3, 0, 0, 0, zone).toInstant().toEpochMilli()
        val evaluation = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(task),
            nowMillis = night,
            zone = zone,
            milesDue = emptyMap(),
            states = emptyMap(),
            quiet = MaintenanceReminderPlanner.QuietWindow(enabled = true, startHour = 22, endHour = 7),
        )
        assertFalse(evaluation.plans.single().notify)
        val expected = ZonedDateTime.of(2026, 10, 29, 7, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, evaluation.alarms.single().triggerAtMillis)
    }

    @Test
    fun notificationsDisabled_schedulesNothing() {
        val evaluation = MaintenanceReminderPlanner.evaluate(
            tasks = listOf(dateTask(due = "2026-10-28"), milesTask()),
            nowMillis = now,
            zone = zone,
            milesDue = mapOf(7L to true),
            states = emptyMap(),
            notificationsEnabled = false,
        )
        assertTrue(evaluation.plans.isEmpty())
        assertTrue(evaluation.alarms.isEmpty())
    }

    @Test
    fun delivery_matchesExactAlarmPermission() {
        assertEquals(
            MaintenanceReminderPlanner.Delivery.EXACT,
            MaintenanceReminderPlanner.delivery(canScheduleExactAlarms = true),
        )
        assertEquals(
            MaintenanceReminderPlanner.Delivery.FALLBACK,
            MaintenanceReminderPlanner.delivery(canScheduleExactAlarms = false),
        )
    }

    private fun dateTask(due: String, repeatDays: Int? = null) = MaintenanceTask(
        id = 4L,
        title = "Oil change",
        startDate = "2026-09-01",
        reminderType = MaintenanceReminderType.DATE,
        dueDate = due,
        repeatIntervalDays = repeatDays,
    )

    private fun milesTask() = MaintenanceTask(
        id = 7L,
        title = "Oil change",
        startDate = "2026-09-01",
        reminderType = MaintenanceReminderType.MILES,
        intervalMiles = 10_000.0,
        odometerAtStart = 100_000.0,
    )

    private fun occurrence(id: Long, trigger: Long, cycle: String) =
        MaintenanceReminderPlanner.Occurrence(
            alarmId = id,
            taskId = id,
            triggerAtMillis = trigger,
            cycleKey = cycle,
            title = "Oil change",
            alerts = true,
        )
}
