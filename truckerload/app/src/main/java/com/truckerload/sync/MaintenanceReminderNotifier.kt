package com.truckerload.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import com.truckerload.presentation.navigation.Routes

/**
 * High-importance channel used only by maintenance reminders.
 * Existing channels ([SmartNotificationWorker.CHANNEL_ALERTS], pickup alarms) are not modified.
 */
object MaintenanceReminderNotifier {

    const val CHANNEL_ID = "truckerload_maintenance_alarm"
    private const val NOTIFY_BASE = 8_000

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun soundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.maintenance_reminder}")

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.maintenance_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.maintenance_reminder_channel_desc)
            setSound(soundUri(context), attributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, userId: String, taskId: Long, title: String): Boolean {
        if (!canPost(context)) return false
        ensureChannel(context)
        val body = context.getString(R.string.notify_maintenance_body, title)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, Routes.MAINTENANCE)
        }
        val content = PendingIntent.getActivity(
            context,
            MaintenanceAlarmScheduler.requestCode(userId, taskId, 4),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val done = MaintenanceAlarmScheduler.actionIntent(
            context,
            MaintenanceAlarmScheduler.ACTION_DONE,
            userId,
            taskId,
        )
        val snooze = MaintenanceAlarmScheduler.actionIntent(
            context,
            MaintenanceAlarmScheduler.ACTION_SNOOZE,
            userId,
            taskId,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notify_maintenance_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri(context))
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(content)
            .addAction(
                0,
                context.getString(R.string.maintenance_reminder_action_done),
                done,
            )
            .addAction(
                0,
                context.getString(R.string.maintenance_reminder_action_snooze),
                snooze,
            )
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        manager.notify(notifyId(userId, taskId), notification)
        return true
    }

    fun cancel(context: Context, userId: String, taskId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(notifyId(userId, taskId))
    }

    fun notifyId(userId: String, taskId: Long): Int {
        val mixed = (userId.hashCode() * 31) xor taskId.hashCode()
        return NOTIFY_BASE + (mixed and 0x0FFF)
    }
}
