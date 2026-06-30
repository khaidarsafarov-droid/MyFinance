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
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.presentation.theme.ThemeManager
import com.truckerload.data.local.AppDatabase
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.sync.TelegramSyncWorker
import com.truckerload.sync.SmartNotificationWorker
import com.truckerload.data.repository.LoadRepository
import com.truckerload.utils.BackupService
import com.truckerload.widget.WidgetStatsLoader
import com.truckerload.widget.WidgetRefresh
import com.truckerload.widget.WidgetUpdateWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import android.widget.Toast

class TruckerLoadApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeManager.apply(AppThemeMode.SYSTEM)
        runBlocking(Dispatchers.IO) {
            runCatching { ThemeManager.apply(SettingsDataStore(this@TruckerLoadApp).getThemeModeOnce()) }
        }
        AppDatabase.getInstance(this)
        TelegramTokenStore(this).syncFromBuildConfig()
        scheduleTelegramSync()
        scheduleTelegramWatchdog()
        scheduleSmartNotifications()
        WidgetUpdateWorker.schedule(this)
        WidgetUpdateWorker.refreshNow(this)
        refreshLoadReportingWeeks()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                TelegramTokenStore(this@TruckerLoadApp).syncFromBuildConfig()
                scheduleTelegramSync()
                WidgetUpdateWorker.refreshNow(this@TruckerLoadApp)
                if (TelegramTokenStore(this@TruckerLoadApp).hasToken()) {
                    TelegramBotForegroundService.start(this@TruckerLoadApp)
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                // Flush widget cache to disk before process may be killed.
                runBlocking(Dispatchers.IO) {
                    runCatching { WidgetStatsLoader.refresh(this@TruckerLoadApp) }
                }
                WidgetRefresh.paintCached(this@TruckerLoadApp)
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

    private fun scheduleTelegramWatchdog() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<TelegramSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "telegram_bot_watchdog",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
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

    private fun refreshLoadReportingWeeks() {
        val db = AppDatabase.getInstance(this)
        val repo = LoadRepository(db)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            BackupService.restoreLatestCompanionBackupIfEmpty(this@TruckerLoadApp)
                ?.onSuccess { message ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TruckerLoadApp, message, Toast.LENGTH_LONG).show()
                    }
                    WidgetUpdateWorker.refreshNow(this@TruckerLoadApp)
                }
            runCatching { repo.backfillPuDelMillisFromStops() }
            runCatching { repo.refreshReportingWeeks() }
                .onSuccess { WidgetUpdateWorker.refreshNow(this@TruckerLoadApp) }
        }
    }
}
