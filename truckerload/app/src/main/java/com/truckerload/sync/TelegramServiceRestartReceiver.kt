package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.truckerload.data.local.DatabaseStartup

/** Legacy receiver — now defers to WorkManager instead of restarting FGS. */
class TelegramServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            try {
                DatabaseStartup.awaitReadyBlockingOrFalse()
                TelegramSyncWorker.enqueueEnsureService(app, replace = true)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
