package com.truckerload.domain.maintenance

/**
 * Which permission prompts to show after a maintenance reminder is saved.
 * Battery exemption is offered once because the WorkManager safety net (and some
 * OEM skins) delay alarms while the app is optimized. Exact-alarm access is the
 * Play-compliant path; it is not granted up front.
 */
object MaintenanceReminderAccess {

    enum class Prompt {
        Notifications,
        NotificationsDenied,
        ExactAlarm,
        Battery,
    }

    data class Snapshot(
        val sdkInt: Int,
        val notificationsGranted: Boolean,
        val canRequestNotifications: Boolean,
        val canScheduleExactAlarms: Boolean,
        val ignoringBatteryOptimizations: Boolean,
        val exactPromptShown: Boolean,
        val batteryPromptShown: Boolean,
    )

    fun prompts(snapshot: Snapshot): List<Prompt> {
        val out = ArrayList<Prompt>()
        if (snapshot.sdkInt >= 33 && !snapshot.notificationsGranted) {
            out += if (snapshot.canRequestNotifications) {
                Prompt.Notifications
            } else {
                Prompt.NotificationsDenied
            }
        }
        if (snapshot.sdkInt >= 31 && !snapshot.canScheduleExactAlarms && !snapshot.exactPromptShown) {
            out += Prompt.ExactAlarm
        }
        if (!snapshot.ignoringBatteryOptimizations && !snapshot.batteryPromptShown) {
            out += Prompt.Battery
        }
        return out
    }
}
