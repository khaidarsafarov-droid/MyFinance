package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Maintenance reminder alarms, notification actions, and clock/package events.
 *
 * [Intent.ACTION_LOCKED_BOOT_COMPLETED] is delivered only because this receiver is
 * direct-boot aware. Before the user unlocks, the handler rearms from device-protected
 * storage and does not open Room.
 */
class MaintenanceReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in ACTIONS) return
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            try {
                MaintenanceReminderCoordinator.onBroadcast(app, intent)
            } catch (error: Exception) {
                Log.e(TAG, "maintenance reminder broadcast failed", error)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        val ACTIONS: Set<String> = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            MaintenanceAlarmScheduler.ACTION_FIRE,
            MaintenanceAlarmScheduler.ACTION_DONE,
            MaintenanceAlarmScheduler.ACTION_SNOOZE,
        )
        private const val TAG = "MaintReminder"
    }
}
