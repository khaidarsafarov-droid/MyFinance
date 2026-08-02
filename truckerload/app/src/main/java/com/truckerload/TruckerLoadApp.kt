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
import com.truckerload.data.preferences.StartupRepairStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.google.android.material.color.DynamicColors
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.presentation.theme.ThemeManager
import com.truckerload.di.UserComponentManager
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.sync.ServerTelegramInboxWorker
import com.truckerload.sync.PushTokenRegistrationWorker
import com.truckerload.sync.TelegramSyncWorker
import com.truckerload.sync.TelegramSyncMode
import com.truckerload.sync.SmartNotificationWorker
import com.truckerload.utils.BackupService
import com.truckerload.utils.CrashReporting
import com.truckerload.widget.WidgetStatsLoader
import com.truckerload.widget.WidgetRefresh
import com.truckerload.widget.WidgetUpdateWorker
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
        appScope.launch(Dispatchers.IO) {
            val language = settingsDataStore.getLanguageOnce()
            SettingsDataStore.mirrorLanguageTag(this@TruckerLoadApp, language.tag)
            val themeMode = runCatching { settingsDataStore.getThemeModeOnce() }
                .getOrDefault(AppThemeMode.SYSTEM)
            withContext(Dispatchers.Main) {
                AppLocale.apply(this@TruckerLoadApp, language)
                ThemeManager.apply(themeMode)
            }
        }
        DynamicColors.applyToActivitiesIfAvailable(this)
        // Do not force SYSTEM here — that races the saved Light/Dark preference and can
        // recreate MainActivity in a loop with Compose's themeMode initialValue.
        if (TelegramSyncMode.isServer()) {
            ServerTelegramInboxWorker.enqueue(this)
            ServerTelegramInboxWorker.enqueuePeriodic(this)
        } else {
            authStore.currentUserIdOrNull()?.let { userId ->
                TelegramTokenStore(this, userId).bootstrapFromBuildConfigIfEmpty()
            }
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
                    PushTokenRegistrationWorker.enqueue(this@TruckerLoadApp)
                    if (TelegramSyncMode.isServer()) {
                        ServerTelegramInboxWorker.enqueue(this@TruckerLoadApp)
                    } else {
                        TelegramTokenStore(this@TruckerLoadApp, userId).bootstrapFromBuildConfigIfEmpty()
                        // Ensure service once; start() is a no-op if already polling.
                        if (TelegramTokenStore(this@TruckerLoadApp, userId).hasToken()) {
                            TelegramBotForegroundService.start(this@TruckerLoadApp)
                        }
                    }
                }
                // Widget refresh is periodic — avoid Room+bitmap work on every resume.
            }

            override fun onStop(owner: LifecycleOwner) {
                WidgetRefresh.paintCached(this@TruckerLoadApp)
                appScope.launch(Dispatchers.IO) {
                    runCatching { WidgetStatsLoader.refresh(this@TruckerLoadApp) }
                        .onFailure { e -> Log.e(TAG, "Widget stats flush failed", e) }
                }
            }
        })
    }

    private fun initializeCrashReporting() {
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("sync_mode", BuildConfig.TELEGRAM_SYNC_MODE)
                setCustomKey("local_only", BuildConfig.LOCAL_ONLY_MODE)
            }
        }.onFailure { error ->
            Log.w(TAG, "Crashlytics initialization failed: ${error.javaClass.simpleName}")
        }
    }

    private fun scheduleTelegramSync() {
        if (TelegramSyncMode.isServer()) {
            ServerTelegramInboxWorker.enqueue(this)
        } else {
            TelegramSyncWorker.enqueueEnsureService(this)
        }
    }

    private fun scheduleTelegramWatchdog() {
        if (TelegramSyncMode.isServer()) {
            ServerTelegramInboxWorker.enqueuePeriodic(this)
            return
        }
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
     * QUALITY_100 #76: startup backfill + orphan cleanup run on [Dispatchers.IO]
     * (never on the main thread from [onCreate]).
     *
     * Backfill success is per-user and recorded only when every step succeeds.
     */
    private fun refreshLoadReportingWeeks() {
        appScope.launch(Dispatchers.IO) {
            val userId = authStore.currentUserIdOrNull() ?: return@launch
            val repo = userComponentManager.startSession(userId).loadRepository
            val repairStore = StartupRepairStore(this@TruckerLoadApp)
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
