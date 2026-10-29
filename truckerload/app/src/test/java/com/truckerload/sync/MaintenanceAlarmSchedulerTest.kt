package com.truckerload.sync

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MaintenanceAlarmSchedulerTest {

    @Test
    fun arm_usesExactWhileIdle_forALaterReminder() {
        val context = context()
        allowExact(context, true)
        val trigger = System.currentTimeMillis() + 3L * 24L * 60L * 60L * 1000L
        val mode = MaintenanceAlarmScheduler.arm(context, "local", occurrence(4L, trigger, alerts = true))
        assertEquals(MaintenanceAlarmScheduler.ArmMode.EXACT_IDLE, mode)
        val alarms = shadowOf(alarmManager(context)).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(trigger, alarms.single().triggerAtTime)
        assertTrue(alarms.single().isAllowWhileIdle)
        assertEquals(null, alarms.single().alarmClockInfo)
    }

    @Test
    fun arm_usesAlarmClock_whenTheRingIsWithinADay() {
        val context = context()
        allowExact(context, true)
        val trigger = System.currentTimeMillis() + 60L * 60L * 1000L
        val mode = MaintenanceAlarmScheduler.arm(context, "local", occurrence(4L, trigger, alerts = true))
        assertEquals(MaintenanceAlarmScheduler.ArmMode.ALARM_CLOCK, mode)
        val alarm = shadowOf(alarmManager(context)).scheduledAlarms.single()
        assertEquals(trigger, alarm.alarmClockInfo?.triggerTime)
    }

    @Test
    fun arm_fallsBack_whenExactAlarmsAreNotPermitted() {
        val context = context()
        allowExact(context, false)
        val trigger = System.currentTimeMillis() + 3L * 24L * 60L * 60L * 1000L
        val mode = MaintenanceAlarmScheduler.arm(context, "local", occurrence(4L, trigger, alerts = true))
        assertEquals(MaintenanceAlarmScheduler.ArmMode.FALLBACK, mode)
        assertFalse(shadowOf(alarmManager(context)).scheduledAlarms.isEmpty())
    }

    @Test
    fun reschedule_cancelsTheStaleAlarm_andKeepsTheReplacement() {
        val context = context()
        allowExact(context, true)
        val soon = System.currentTimeMillis() + 3L * 24L * 60L * 60L * 1000L
        val later = soon + 86_400_000L
        MaintenanceAlarmScheduler.arm(context, "local", occurrence(1L, soon, alerts = true))
        MaintenanceAlarmScheduler.arm(context, "local", occurrence(2L, soon, alerts = true))
        val armed = listOf(
            occurrence(1L, soon, alerts = true),
            occurrence(2L, soon, alerts = true),
        )
        val desired = listOf(occurrence(2L, later, alerts = true))
        val reconcile = MaintenanceReminderPlanner.reconcile(armed, desired)
        reconcile.cancelIds.forEach { id -> MaintenanceAlarmScheduler.cancel(context, "local", id) }
        reconcile.arm.forEach { MaintenanceAlarmScheduler.arm(context, "local", it) }
        val triggers = shadowOf(alarmManager(context)).scheduledAlarms.map { it.triggerAtTime }
        assertEquals(listOf(later), triggers)
    }

    @Test
    fun receiver_handlesBootPackageAndClockChanges() {
        assertTrue(MaintenanceReminderReceiver.ACTIONS.contains(android.content.Intent.ACTION_BOOT_COMPLETED))
        assertTrue(MaintenanceReminderReceiver.ACTIONS.contains(android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED))
        assertTrue(MaintenanceReminderReceiver.ACTIONS.contains(android.content.Intent.ACTION_MY_PACKAGE_REPLACED))
        assertTrue(MaintenanceReminderReceiver.ACTIONS.contains(android.content.Intent.ACTION_TIME_CHANGED))
        assertTrue(MaintenanceReminderReceiver.ACTIONS.contains(android.content.Intent.ACTION_TIMEZONE_CHANGED))
        assertTrue(MaintenanceReminderReceiver.ACTIONS.contains(MaintenanceAlarmScheduler.ACTION_DONE))
        assertTrue(MaintenanceReminderReceiver.ACTIONS.contains(MaintenanceAlarmScheduler.ACTION_SNOOZE))
    }

    @Test
    fun maintenanceChannel_isSeparate_andActionsAreLocalized() {
        assertEquals("truckerload_maintenance_alarm", MaintenanceReminderNotifier.CHANNEL_ID)
        assertFalse(MaintenanceReminderNotifier.CHANNEL_ID == SmartNotificationWorker.CHANNEL_ALERTS)
        assertFalse(MaintenanceReminderNotifier.CHANNEL_ID == SmartNotificationWorker.CHANNEL_MISSING)
        assertFalse(MaintenanceReminderNotifier.CHANNEL_ID == LoadAlarmReceiver.CHANNEL_ID)
        val context = context()
        val russian = localized(context, Locale("ru"))
        assertEquals("Готово", russian.getString(com.truckerload.R.string.maintenance_reminder_action_done))
        assertEquals("Отложить", russian.getString(com.truckerload.R.string.maintenance_reminder_action_snooze))
        val spanish = localized(context, Locale("es"))
        assertEquals("Listo", spanish.getString(com.truckerload.R.string.maintenance_reminder_action_done))
        assertEquals("Posponer", spanish.getString(com.truckerload.R.string.maintenance_reminder_action_snooze))
        val french = localized(context, Locale("fr"))
        assertEquals("Terminé", french.getString(com.truckerload.R.string.maintenance_reminder_action_done))
        assertEquals("Reporter", french.getString(com.truckerload.R.string.maintenance_reminder_action_snooze))
    }

    private fun context(): Application = ApplicationProvider.getApplicationContext()

    private fun alarmManager(context: Application): AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    private fun allowExact(context: Application, allowed: Boolean) {
        ShadowAlarmManager.setCanScheduleExactAlarms(allowed)
    }

    private fun occurrence(id: Long, trigger: Long, alerts: Boolean) =
        MaintenanceReminderPlanner.Occurrence(
            alarmId = id,
            taskId = id,
            triggerAtMillis = trigger,
            cycleKey = "date:2026-11-01",
            title = "Oil change",
            alerts = alerts,
        )

    private fun localized(context: Application, locale: Locale): android.content.Context {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
