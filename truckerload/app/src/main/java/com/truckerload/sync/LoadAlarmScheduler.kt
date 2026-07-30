package com.truckerload.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.truckerload.data.preferences.LoadAlarmStore
import com.truckerload.data.preferences.PendingLoadAlarm
import com.truckerload.domain.model.Load

/** Schedules / cancels exact pickup reminder alarms via [AlarmManager]. */
object LoadAlarmScheduler {

    const val ACTION_LOAD_ALARM = "com.truckerload.action.LOAD_PICKUP_ALARM"
    const val EXTRA_LOAD_ID = "load_id"
    const val EXTRA_TRIP_ID = "trip_id"
    const val EXTRA_PICKUP_MILLIS = "pickup_millis"
    const val EXTRA_ROUTE_LABEL = "route_label"

    fun schedule(
        context: Context,
        load: Load,
        triggerAtMillis: Long,
        pickupMillis: Long,
    ): Boolean {
        if (triggerAtMillis <= System.currentTimeMillis()) return false
        val appContext = context.applicationContext
        val routeLabel = buildRouteLabel(load)
        val pending = PendingLoadAlarm(
            loadId = load.id,
            tripId = load.tripId,
            triggerAtMillis = triggerAtMillis,
            pickupMillis = pickupMillis,
            routeLabel = routeLabel,
        )
        LoadAlarmStore(appContext).upsert(pending)
        return scheduleExact(appContext, pending)
    }

    fun cancel(context: Context, loadId: String) {
        val appContext = context.applicationContext
        LoadAlarmStore(appContext).remove(loadId)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(pendingIntent(appContext, loadId, tripId = "", pickupMillis = 0L, routeLabel = ""))
    }

    /** Re-arm all stored future alarms (e.g. after BOOT_COMPLETED). */
    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        val store = LoadAlarmStore(appContext)
        val now = System.currentTimeMillis()
        val stillValid = store.getAll().filter { it.triggerAtMillis > now }
        store.getAll().filter { it.triggerAtMillis <= now }.forEach { store.remove(it.loadId) }
        stillValid.forEach { scheduleExact(appContext, it) }
    }

    private fun scheduleExact(context: Context, alarm: PendingLoadAlarm): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val pending = pendingIntent(
            context,
            alarm.loadId,
            alarm.tripId,
            alarm.pickupMillis,
            alarm.routeLabel,
        )
        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.triggerAtMillis,
                pending,
            )
            true
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.triggerAtMillis,
                pending,
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun pendingIntent(
        context: Context,
        loadId: String,
        tripId: String,
        pickupMillis: Long,
        routeLabel: String,
    ): PendingIntent {
        val intent = Intent(context, LoadAlarmReceiver::class.java).apply {
            action = ACTION_LOAD_ALARM
            putExtra(EXTRA_LOAD_ID, loadId)
            putExtra(EXTRA_TRIP_ID, tripId)
            putExtra(EXTRA_PICKUP_MILLIS, pickupMillis)
            putExtra(EXTRA_ROUTE_LABEL, routeLabel)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode(loadId), intent, flags)
    }

    fun requestCode(loadId: String): Int = loadId.hashCode() and 0x7FFFFFFF

    private fun buildRouteLabel(load: Load): String {
        val fromStops = listOf(load.firstPuCityState, load.lastDelCityState)
            .filter { it.isNotBlank() }
            .joinToString(" → ")
        if (fromStops.isNotBlank()) return fromStops
        if (load.route.isNotBlank()) return load.route
        return listOf(load.pointA, load.pointB).filter { it.isNotBlank() }.joinToString(" → ")
    }
}
