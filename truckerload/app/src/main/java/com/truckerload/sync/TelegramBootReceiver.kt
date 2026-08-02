package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore

/** Перезапуск Telegram polling после перезагрузки телефона (только если есть активный аккаунт). */
class TelegramBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        LoadAlarmScheduler.rescheduleAll(appContext)
        if (TelegramSyncMode.isServer()) {
            ServerTelegramInboxWorker.enqueue(appContext)
            ServerTelegramInboxWorker.enqueuePeriodic(appContext)
            return
        }
        val userId = AuthStore(appContext).currentUserIdOrNull() ?: return
        val tokenStore = TelegramTokenStore(appContext, userId)
        tokenStore.bootstrapFromBuildConfigIfEmpty()
        if (!tokenStore.hasToken()) return
        // Android 15+: dataSync FGS must not be started from BOOT_COMPLETED.
        // Defer via WorkManager; the service resumes when the user opens the app if needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            TelegramSyncWorker.enqueueEnsureService(appContext)
        } else {
            TelegramBotForegroundService.start(appContext)
        }
    }
}
