package com.truckerload

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.truckerload.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.repository.LoadPendingDeleteApplier
import com.truckerload.data.preferences.StartupRepairStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.google.android.material.color.DynamicColors
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.presentation.theme.ThemeManager
import com.truckerload.di.UserComponentManager
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.sync.TelegramSyncWorker
import com.truckerload.sync.SmartNotificationWorker
import com.truckerload.utils.BackupService
import com.truckerload.utils.CrashReporting
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetRefresh
import com.truckerload.widget.WidgetUpdateWorker
import com.truckerload.utils.AppLanguageManager
import com.truckerload.utils.AppLocale
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

@HiltAndroidApp
class TruckerLoadApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var authStore: AuthStore

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var userComponentManager: UserComponentManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        initializeCrashReporting()
        // FIX: hydrate durable dataSync FGS pause before watchdog/boot can restart the bot
        com.truckerload.sync.TelegramFgsQuota.init(this)
        appScope.launch(Dispatchers.IO) {
            WeekStartRuntime.install(
                settingsDataStore.getLoadWeekStartDayOnce(),
                settingsDataStore.getDieselWeekStartDayOnce(),
            )
            val explicitLanguage = settingsDataStore.getExplicitLanguageOnce()
            val legacyTag = SettingsDataStore.readLegacyLanguageTag(this@TruckerLoadApp)
            val themeMode = runCatching { settingsDataStore.getThemeModeOnce() }
                .getOrDefault(AppThemeMode.SYSTEM)
            withContext(Dispatchers.Main) {
                AppLanguageManager.migrateLegacyIfNeeded(explicitLanguage?.tag ?: legacyTag)
                ThemeManager.apply(themeMode)
            }
        }
        DynamicColors.applyToActivitiesIfAvailable(this)
        cancelRetiredCloudWork()
        // Do not force SYSTEM here — that races the saved Light/Dark preference and can
        // recreate MainActivity in a loop with Compose's themeMode initialValue.
        authStore.currentUserIdOrNull()?.let { userId ->
            TelegramTokenStore(this, userId).bootstrapFromBuildConfigIfEmpty()
        }
        scheduleTelegramSync()
        scheduleTelegramWatchdog()
        scheduleSmartNotifications()
        WidgetUpdateWorker.schedule(this)
        // One deferred widget paint at cold start; periodic worker keeps it fresh.
        WidgetUpdateWorker.refreshNow(this)
        refreshLoadReportingWeeks()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val userId = authStore.currentUserIdOrNull()
                if (userId != null) {
                    TelegramTokenStore(this@TruckerLoadApp, userId).bootstrapFromBuildConfigIfEmpty()
                    // Ensure service once; start() is a no-op if already polling.
                    if (TelegramTokenStore(this@TruckerLoadApp, userId).hasToken()) {
                        TelegramBotForegroundService.start(this@TruckerLoadApp)
                    }
                }
                WidgetDataUpdater.updateWidgetData(this@TruckerLoadApp)
            }

            override fun onStop(owner: LifecycleOwner) {
                WidgetRefresh.flushForHomeScreen(this@TruckerLoadApp)
            }
        })
    }

    private fun initializeCrashReporting() {
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("local_only", BuildConfig.LOCAL_ONLY_MODE)
            }
        }.onFailure { error ->
            Log.w(TAG, "Crashlytics initialization failed: ${error.javaClass.simpleName}")
        }
    }

    private fun scheduleTelegramSync() {
        TelegramSyncWorker.enqueueEnsureService(this)
    }

    private fun scheduleTelegramWatchdog() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<TelegramSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TelegramSyncWorker.UNIQUE_WATCHDOG_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
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

    /**
     * Drop leftover WorkManager jobs from the removed Ktor/Supabase cloud stack
     * so upgrades do not keep retrying missing worker classes.
     */
    private fun cancelRetiredCloudWork() {
        val wm = WorkManager.getInstance(this)
        listOf(
            "cloud_sync_oneshot",
            "cloud_sync_periodic",
            "outbound_sync_drain",
            "media_cloud_sync_oneshot",
            "media_cloud_sync_periodic",
            "push_token_registration",
            "server_telegram_inbox_oneshot",
            "server_telegram_inbox_periodic",
        ).forEach { name ->
            wm.cancelUniqueWork(name)
        }
    }

    /**
     * QUALITY_100 #76: startup backfill + orphan cleanup run on [Dispatchers.IO]
     * (never on the main thread from [onCreate]).
     *
     * Backfill success is per-user and recorded only when every step succeeds.
     */
    private fun refreshLoadReportingWeeks() {
        appScope.launch(Dispatchers.IO) {
            WeekStartRuntime.install(
                settingsDataStore.getLoadWeekStartDayOnce(),
                settingsDataStore.getDieselWeekStartDayOnce(),
            )
            val userId = authStore.currentUserIdOrNull() ?: return@launch
            val repo = userComponentManager.startSession(userId).loadRepository
            val repairStore = StartupRepairStore(this@TruckerLoadApp)
            runCatching { LoadPendingDeleteApplier.apply(repo) }
                .onFailure { e -> Log.e(TAG, "Pending load deletes failed", e) }
            BackupService.restoreLatestCompanionBackupIfEmpty(this@TruckerLoadApp)
                ?.onSuccess { message ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TruckerLoadApp, message, Toast.LENGTH_LONG).show()
                    }
                    WidgetUpdateWorker.refreshNow(this@TruckerLoadApp)
                }
            if (!repairStore.isBackfillDone(userId)) {
                val puDel = runCatching { repo.backfillPuDelMillisFromStops() }
                    .onFailure { e ->
                        Log.e(TAG, "PU/DEL backfill failed", e)
                        CrashReporting.recordException(e)
                    }
                val weeks = runCatching { repo.refreshReportingWeeks() }
                    .onFailure { e ->
                        Log.e(TAG, "Reporting weeks refresh failed", e)
                        CrashReporting.recordException(e)
                    }
                if (puDel.isSuccess && weeks.isSuccess) {
                    repairStore.markBackfillDone(userId)
                    WidgetUpdateWorker.refreshNow(this@TruckerLoadApp)
                } else {
                    repairStore.markBackfillNeedsRetry(userId)
                    CrashReporting.setCustomKey("startup_backfill_user", userId)
                    CrashReporting.setCustomKey("startup_backfill_failed", true)
                }
            }
            runCatching { repo.cleanupOrphanAttachments() }
                .onFailure { e -> Log.e(TAG, "Orphan attachment cleanup failed", e) }
        }
    }

    companion object {
        private const val TAG = "TruckerLoadApp"
    }
}
