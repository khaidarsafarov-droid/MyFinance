package com.truckerload.sync

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner.Occurrence
import java.util.concurrent.TimeUnit

/**
 * Arms one alarm per reminder (and one daily miles anchor).
 * Exact path: [AlarmManager.setAlarmClock] when the ring is within a day, otherwise
 * [AlarmManager.setExactAndAllowWhileIdle]. Both still fire in Doze when exact alarms
 * are allowed. Otherwise an inexact while-idle alarm plus a WorkManager one-shot.
 */
object MaintenanceAlarmScheduler {

    const val ACTION_FIRE = "com.truckorig.action.MAINTENANCE_REMINDER"
    const val ACTION_DONE = "com.truckorig.action.MAINTENANCE_REMINDER_DONE"
    const val ACTION_SNOOZE = "com.truckorig.action.MAINTENANCE_REMINDER_SNOOZE"
    const val EXTRA_USER_ID = "user_id"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_TASK_ID = "task_id"
    const val SAFETY_WORK = "maintenance_reminder_safety"

    enum class ArmMode {
        ALARM_CLOCK,
        EXACT_IDLE,
        FALLBACK,
    }

    @SuppressLint("MissingPermission")
    fun arm(context: Context, userId: String, occurrence: Occurrence): ArmMode {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return ArmMode.FALLBACK
        val pending = fireIntent(app, userId, occurrence)
        val now = System.currentTimeMillis()
        val canExact = ExactAlarmAccess.canScheduleExact(app)
        val soon = occurrence.alerts &&
            occurrence.triggerAtMillis - now <= MaintenanceReminderPlanner.SOON_WINDOW_MILLIS
        if (canExact) {
            try {
                if (soon) {
                    val show = contentIntent(app, userId, occurrence.taskId, SLOT_CONTENT)
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(occurrence.triggerAtMillis, show),
                        pending,
                    )
                    cancelOneShot(app, userId, occurrence.alarmId)
                    return ArmMode.ALARM_CLOCK
                }
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    occurrence.triggerAtMillis,
                    pending,
                )
                cancelOneShot(app, userId, occurrence.alarmId)
                return ArmMode.EXACT_IDLE
            } catch (e: SecurityException) {
                Log.w(TAG, "exact alarm denied", e)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                occurrence.triggerAtMillis,
                pending,
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, occurrence.triggerAtMillis, pending)
        }
        enqueueOneShot(app, userId, occurrence)
        return ArmMode.FALLBACK
    }

    fun cancel(context: Context, userId: String, alarmId: Long, taskId: Long = alarmId) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return
        val occurrence = Occurrence(
            alarmId = alarmId,
            taskId = taskId,
            triggerAtMillis = 0L,
            cycleKey = "",
            title = "",
            alerts = false,
        )
        alarmManager.cancel(fireIntent(app, userId, occurrence))
        cancelOneShot(app, userId, alarmId)
    }

    fun ensureSafetyNet(context: Context) {
        val app = context.applicationContext
        val request = PeriodicWorkRequestBuilder<MaintenanceReminderSafetyWorker>(6, TimeUnit.HOURS)
            .build()
        runCatching {
            WorkManager.getInstance(app).enqueueUniquePeriodicWork(
                SAFETY_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }.onFailure { error ->
            Log.w(TAG, "safety net not enqueued", error)
        }
    }

    fun fireIntent(context: Context, userId: String, occurrence: Occurrence): PendingIntent {
        val intent = Intent(context, MaintenanceReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_ALARM_ID, occurrence.alarmId)
            putExtra(EXTRA_TASK_ID, occurrence.taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(userId, occurrence.alarmId, SLOT_FIRE),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun actionIntent(
        context: Context,
        action: String,
        userId: String,
        taskId: Long,
    ): PendingIntent {
        val intent = Intent(context, MaintenanceReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_ALARM_ID, taskId)
        }
        val slot = if (action == ACTION_DONE) SLOT_DONE else SLOT_SNOOZE
        return PendingIntent.getBroadcast(
            context,
            requestCode(userId, taskId, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun requestCode(userId: String, alarmId: Long, slot: Int): Int {
        val mixed = (userId.hashCode() xor alarmId.hashCode()) and 0x00FFFFFF
        return (slot shl 24) or mixed
    }

    private fun contentIntent(
        context: Context,
        userId: String,
        taskId: Long,
        slot: Int,
    ): PendingIntent {
        val intent = Intent(context, com.truckerload.presentation.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                com.truckerload.presentation.MainActivity.EXTRA_ROUTE,
                com.truckerload.presentation.navigation.Routes.MAINTENANCE,
            )
        }
        return PendingIntent.getActivity(
            context,
            requestCode(userId, taskId, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun enqueueOneShot(context: Context, userId: String, occurrence: Occurrence) {
        val delay = (occurrence.triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<MaintenanceReminderSafetyWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                oneShotName(userId, occurrence.alarmId),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }.onFailure { error ->
            Log.w(TAG, "fallback work not enqueued", error)
        }
    }

    private fun cancelOneShot(context: Context, userId: String, alarmId: Long) {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(oneShotName(userId, alarmId))
        }
    }

    private fun oneShotName(userId: String, alarmId: Long): String =
        "maint_rem_${userId.hashCode()}_$alarmId"

    private const val SLOT_FIRE = 1
    private const val SLOT_DONE = 2
    private const val SLOT_SNOOZE = 3
    private const val SLOT_CONTENT = 4
    private const val TAG = "MaintAlarm"
}
