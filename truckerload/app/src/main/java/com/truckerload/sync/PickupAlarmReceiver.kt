package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires a pickup reminder notification when the scheduled alarm triggers. */
class PickupAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PickupAlarmScheduler.ACTION_PICKUP_ALARM) return
        val loadId = intent.getStringExtra(PickupAlarmScheduler.EXTRA_LOAD_ID) ?: return
        val tripId = intent.getStringExtra(PickupAlarmScheduler.EXTRA_TRIP_ID).orEmpty()
        val pickupMillis = intent.getLongExtra(PickupAlarmScheduler.EXTRA_PICKUP_MILLIS, 0L)
        if (pickupMillis <= 0L) return
        PickupAlarmScheduler.showNotification(context.applicationContext, loadId, tripId, pickupMillis)
    }
}
