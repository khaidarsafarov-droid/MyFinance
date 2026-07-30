package com.truckerload.sync

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.truckerload.R

/** Schedules and cancels exact pickup reminders via [AlarmManager]. */
object PickupAlarmScheduler {

    const val CHANNEL_PICKUP = "truckerload_pickup"
    const val ACTION_PICKUP_ALARM = "com.truckerload.action.PICKUP_ALARM"
    const val EXTRA_LOAD_ID = "load_id"
    const val EXTRA_TRIP_ID = "trip_id"
    const val EXTRA_PICKUP_MILLIS = "pickup_millis"
    const val EXTRA_ALARM_MILLIS = "alarm_millis"

    private const val NOTIFICATION_ID_BASE = 5_000

    fun schedule(
        context: Context,
        loadId: String,
        tripId: String,
        pickupMillis: Long,
        alarmMillis: Long,
    ): Boolean {
        if (alarmMillis <= System.currentTimeMillis()) return false
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val intent = alarmIntent(context, loadId, tripId, pickupMillis, alarmMillis)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(loadId),
            intent,
            flags,
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmMillis, pending)
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmMillis, pending)
        }
        return true
    }

    fun cancel(context: Context, loadId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PickupAlarmReceiver::class.java).apply {
            action = ACTION_PICKUP_ALARM
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getBroadcast(context, requestCode(loadId), intent, flags)
        alarmManager.cancel(pending)
        pending.cancel()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId(loadId))
    }

    fun showNotification(
        context: Context,
        loadId: String,
        tripId: String,
        pickupMillis: Long,
    ) {
        ensureChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pickupLabel = com.truckerload.utils.formatDateTimeForDisplay(pickupMillis)
        val notification = NotificationCompat.Builder(context, CHANNEL_PICKUP)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.pickup_alarm_notify_title))
            .setContentText(context.getString(R.string.pickup_alarm_notify_body, tripId, pickupLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(notificationId(loadId), notification)
    }

    internal fun alarmIntent(
        context: Context,
        loadId: String,
        tripId: String,
        pickupMillis: Long,
        alarmMillis: Long,
    ): Intent = Intent(context, PickupAlarmReceiver::class.java).apply {
        action = ACTION_PICKUP_ALARM
        putExtra(EXTRA_LOAD_ID, loadId)
        putExtra(EXTRA_TRIP_ID, tripId)
        putExtra(EXTRA_PICKUP_MILLIS, pickupMillis)
        putExtra(EXTRA_ALARM_MILLIS, alarmMillis)
    }

    private fun requestCode(loadId: String): Int = loadId.hashCode() and 0x7FFF

    private fun notificationId(loadId: String): Int = NOTIFICATION_ID_BASE + (loadId.hashCode() and 0x7FFF)

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PICKUP,
                context.getString(R.string.pickup_alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }
}
