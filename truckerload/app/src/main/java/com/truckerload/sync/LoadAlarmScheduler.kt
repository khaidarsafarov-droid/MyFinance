package com.truckerload.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/** Schedules exact alarms that fire [LoadAlarmReceiver] for upcoming load pickups. */
object LoadAlarmScheduler {

    const val ACTION_LOAD_ALARM = "com.truckerload.action.LOAD_PICKUP_ALARM"
    const val EXTRA_LOAD_ID = "load_id"
    const val EXTRA_TRIP_ID = "trip_id"
    const val EXTRA_PICKUP_MILLIS = "pickup_millis"

    fun schedule(
        context: Context,
        loadId: String,
        tripId: String,
        triggerAtMillis: Long,
        pickupMillis: Long,
    ): Boolean {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        if (!LoadAlarmPlanner.isValidAlarmTime(triggerAtMillis, pickupMillis, now)) {
            return false
        }
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val pending = pendingIntent(app, loadId, tripId, pickupMillis)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending,
                )
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "exact alarm denied, falling back", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
        LoadAlarmStore(app).put(
            LoadAlarmStore.Entry(
                loadId = loadId,
                tripId = tripId,
                triggerAtMillis = triggerAtMillis,
                pickupMillis = pickupMillis,
            ),
        )
        return true
    }

    fun cancel(context: Context, loadId: String) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val stored = LoadAlarmStore(app).get(loadId)
        val pending = pendingIntent(
            app,
            loadId,
            stored?.tripId.orEmpty(),
            stored?.pickupMillis ?: 0L,
        )
        alarmManager.cancel(pending)
        LoadAlarmStore(app).remove(loadId)
    }

    /** Re-arm future alarms after reboot (past ones are dropped). */
    fun rescheduleAll(context: Context) {
        val app = context.applicationContext
        val store = LoadAlarmStore(app)
        val now = System.currentTimeMillis()
        store.all().forEach { entry ->
            if (entry.triggerAtMillis <= now) {
                store.remove(entry.loadId)
                return@forEach
            }
            schedule(
                context = app,
                loadId = entry.loadId,
                tripId = entry.tripId,
                triggerAtMillis = entry.triggerAtMillis,
                pickupMillis = entry.pickupMillis,
            )
        }
    }

    private fun pendingIntent(
        context: Context,
        loadId: String,
        tripId: String,
        pickupMillis: Long,
    ): PendingIntent {
        val intent = Intent(context, LoadAlarmReceiver::class.java).apply {
            action = ACTION_LOAD_ALARM
            putExtra(EXTRA_LOAD_ID, loadId)
            putExtra(EXTRA_TRIP_ID, tripId)
            putExtra(EXTRA_PICKUP_MILLIS, pickupMillis)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode(loadId), intent, flags)
    }

    fun requestCode(loadId: String): Int = loadId.hashCode() and 0x7FFFFFFF

    private const val TAG = "LoadAlarm"
}
