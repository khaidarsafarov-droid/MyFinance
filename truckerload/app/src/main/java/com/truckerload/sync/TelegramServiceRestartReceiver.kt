package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Legacy receiver — now defers to WorkManager instead of restarting FGS. */
class TelegramServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        TelegramSyncWorker.enqueueEnsureService(context.applicationContext, replace = true)
    }
}
