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
import com.truckerload.data.preferences.LoadAlarmStore
import com.truckerload.presentation.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fires the pickup reminder notification when [LoadAlarmScheduler] triggers. */
class LoadAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != LoadAlarmScheduler.ACTION_LOAD_ALARM) return
        val appContext = context.applicationContext
        val loadId = intent.getStringExtra(LoadAlarmScheduler.EXTRA_LOAD_ID).orEmpty()
        val tripId = intent.getStringExtra(LoadAlarmScheduler.EXTRA_TRIP_ID).orEmpty()
        val pickupMillis = intent.getLongExtra(LoadAlarmScheduler.EXTRA_PICKUP_MILLIS, 0L)
        val routeLabel = intent.getStringExtra(LoadAlarmScheduler.EXTRA_ROUTE_LABEL).orEmpty()

        if (loadId.isNotBlank()) {
            LoadAlarmStore(appContext).remove(loadId)
        }

        ensureChannel(appContext)
        val title = appContext.getString(R.string.load_alarm_notify_title)
        val timeLabel = if (pickupMillis > 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(pickupMillis))
        } else {
            ""
        }
        val body = when {
            tripId.isNotBlank() && routeLabel.isNotBlank() && timeLabel.isNotBlank() ->
                appContext.getString(R.string.load_alarm_notify_body_full, tripId, routeLabel, timeLabel)
            tripId.isNotBlank() && timeLabel.isNotBlank() ->
                appContext.getString(R.string.load_alarm_notify_body_trip_time, tripId, timeLabel)
            timeLabel.isNotBlank() ->
                appContext.getString(R.string.load_alarm_notify_body_time, timeLabel)
            else ->
                appContext.getString(R.string.load_alarm_notify_body_generic)
        }

        val openApp = PendingIntent.getActivity(
            appContext,
            LoadAlarmScheduler.requestCode(loadId),
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_BASE_ID + LoadAlarmScheduler.requestCode(loadId) % 10_000, notification)
    }

    private fun ensureChannel(context: Context) {
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
        private const val NOTIFICATION_BASE_ID = 42000
    }
}
