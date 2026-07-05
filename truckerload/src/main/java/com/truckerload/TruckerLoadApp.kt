package com.truckerload

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.truckerload.data.local.AppDatabase
import com.truckerload.sync.TelegramSyncWorker
import com.truckerload.sync.SmartNotificationWorker
import java.util.concurrent.TimeUnit

class TruckerLoadApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
        scheduleTelegramSync()
        scheduleSmartNotifications()
        // Offline-first: синхронизация при возврате в приложение (мгновенное обновление после бота)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                scheduleTelegramSync()
            }
        })
    }

    private fun scheduleTelegramSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<TelegramSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueue(request)
    }

    private fun scheduleSmartNotifications() {
        val request = PeriodicWorkRequestBuilder<SmartNotificationWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "smart_notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
