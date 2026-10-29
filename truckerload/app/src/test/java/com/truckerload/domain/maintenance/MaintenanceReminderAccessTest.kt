package com.truckerload.domain.maintenance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceReminderAccessTest {

    @Test
    fun firstReminder_onAndroid13_asksForNotifications_thenExactAndBattery() {
        val prompts = MaintenanceReminderAccess.prompts(
            MaintenanceReminderAccess.Snapshot(
                sdkInt = 33,
                notificationsGranted = false,
                canRequestNotifications = true,
                canScheduleExactAlarms = false,
                ignoringBatteryOptimizations = false,
                exactPromptShown = false,
                batteryPromptShown = false,
            ),
        )
        assertEquals(
            listOf(
                MaintenanceReminderAccess.Prompt.Notifications,
                MaintenanceReminderAccess.Prompt.ExactAlarm,
                MaintenanceReminderAccess.Prompt.Battery,
            ),
            prompts,
        )
    }

    @Test
    fun deniedNotifications_areExplained_withoutAskingAgain() {
        val prompts = MaintenanceReminderAccess.prompts(base().copy(canRequestNotifications = false))
        assertEquals(
            listOf(
                MaintenanceReminderAccess.Prompt.NotificationsDenied,
                MaintenanceReminderAccess.Prompt.ExactAlarm,
                MaintenanceReminderAccess.Prompt.Battery,
            ),
            prompts,
        )
    }

    @Test
    fun grantedAndExactAndUnoptimized_showsNothing() {
        val prompts = MaintenanceReminderAccess.prompts(
            base().copy(
                notificationsGranted = true,
                canScheduleExactAlarms = true,
                ignoringBatteryOptimizations = true,
            ),
        )
        assertTrue(prompts.isEmpty())
    }

    @Test
    fun promptsAreNotRepeated() {
        val prompts = MaintenanceReminderAccess.prompts(
            base().copy(
                notificationsGranted = true,
                exactPromptShown = true,
                batteryPromptShown = true,
            ),
        )
        assertTrue(prompts.isEmpty())
    }

    private fun base() = MaintenanceReminderAccess.Snapshot(
        sdkInt = 34,
        notificationsGranted = false,
        canRequestNotifications = false,
        canScheduleExactAlarms = false,
        ignoringBatteryOptimizations = false,
        exactPromptShown = false,
        batteryPromptShown = false,
    )
}
