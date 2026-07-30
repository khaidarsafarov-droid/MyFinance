package com.truckerload.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fires a high-priority notification when a scheduled load pickup alarm triggers. */
class LoadAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != LoadAlarmScheduler.ACTION_LOAD_ALARM) return
        val app = context.applicationContext
        val loadId = intent.getStringExtra(LoadAlarmScheduler.EXTRA_LOAD_ID).orEmpty()
        val tripId = intent.getStringExtra(LoadAlarmScheduler.EXTRA_TRIP_ID).orEmpty()
        val pickupMillis = intent.getLongExtra(LoadAlarmScheduler.EXTRA_PICKUP_MILLIS, 0L)
        if (loadId.isNotEmpty()) {
            LoadAlarmStore(app).remove(loadId)
        }
        showNotification(app, loadId, tripId, pickupMillis)
    }

    private fun showNotification(
        context: Context,
        loadId: String,
        tripId: String,
        pickupMillis: Long,
    ) {
        createChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            LoadAlarmScheduler.requestCode(loadId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pickupLabel = if (pickupMillis > 0L) {
            TIME_FMT.format(Date(pickupMillis))
        } else {
            "—"
        }
        val title = context.getString(R.string.load_alarm_notify_title)
        val body = if (tripId.isNotBlank()) {
            context.getString(R.string.load_alarm_notify_body, tripId, pickupLabel)
        } else {
            context.getString(R.string.load_alarm_notify_body_no_trip, pickupLabel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifyId = if (loadId.isNotEmpty()) {
            NOTIFY_BASE + (loadId.hashCode() and 0x0FFF)
        } else {
            NOTIFY_BASE
        }
        nm.notify(notifyId, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.load_alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.load_alarm_channel_desc)
                enableVibration(true)
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "truckerload_load_alarms"
        private const val NOTIFY_BASE = 4200
        private val TIME_FMT = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    }
}
