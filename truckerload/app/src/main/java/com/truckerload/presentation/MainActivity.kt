package com.truckerload.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.Trace
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.StartupRepairStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.di.UserComponent
import com.truckerload.di.UserComponentManager
import com.truckerload.utils.CrashReporting
import com.truckerload.presentation.components.AutoRestoreDialog
import com.truckerload.presentation.components.LegacyAbsorbDialog
import com.truckerload.sync.SessionTeardown
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalAnalyticsRepository
import com.truckerload.presentation.di.LocalAuthCredentialsStore
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalMaintenanceRepository
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.di.LocalPhotoRepository
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.presentation.di.LocalSelectedStateStore
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.di.LocalProfileRepository
import com.truckerload.presentation.di.LocalSocialSyncCoordinator
import com.truckerload.presentation.di.LocalStatsSelectionStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.di.LocalWeekRepository
import com.truckerload.presentation.di.LocalLastUsedDefaultsStore
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.navigation.AuthNavHost
import com.truckerload.presentation.navigation.NavGraph
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.sync.PushTokenRegistrationWorker
import com.truckerload.sync.ServerTelegramInboxWorker
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.sync.TelegramSyncMode
import com.truckerload.utils.AppLocale
import com.truckerload.utils.FeedbackManager
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetDeepLink
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var authStore: AuthStore

    @Inject
    lateinit var authCredentialsStore: AuthCredentialsStore

    @Inject
    lateinit var userProfileStore: UserProfileStore

    @Inject
    lateinit var userComponentManager: UserComponentManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    private var deepLinkRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkRoute(intent.getStringExtra(EXTRA_ROUTE))
        WidgetDataUpdater.updateWidgetData(applicationContext)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        FeedbackManager.init(applicationContext)

        setContent {
            var session by remember { mutableStateOf<UserComponent?>(null) }
            var sessionReady by remember { mutableStateOf(false) }
            val isLoggedIn by authStore.isLoggedIn.collectAsStateWithLifecycle()
            val userId by authStore.userId.collectAsStateWithLifecycle()

            LaunchedEffect(isLoggedIn, userId) {
                // Only ephemeral guest / local_dev must be rejected. Email (`local_*` /
                // Supabase UUID) and Google sessions must survive cold start.
                val provider = authStore.authProvider()
                val isGuestLocal = userId == AccountIds.LOCAL_DEV ||
                    (provider == AuthProvider.LOCAL && userId.isNullOrBlank())
                if (isLoggedIn && isGuestLocal) {
                    authStore.logout()
                    userComponentManager.endSession()
                    session = null
                    sessionReady = true
                    return@LaunchedEffect
                }
                if (isLoggedIn && !userId.isNullOrBlank()) {
                    val activeUserId = userId as String
                    val needsRebuild = session == null || session?.userId != activeUserId
                    if (needsRebuild) {
                        if (session == null) {
                            sessionReady = false
                        }
                        val deps = withContext(Dispatchers.IO) {
                            userComponentManager.startSession(activeUserId)
                        }
                        session = deps
                        if (!TelegramSyncMode.isServer()) {
                            // FIX: stop previous account's poller before starting the new session's bot
                            TelegramBotForegroundService.stopForLogout(applicationContext)
                            val tokenStore = TelegramTokenStore(applicationContext, activeUserId)
                            tokenStore.bootstrapFromBuildConfigIfEmpty()
                            if (tokenStore.hasToken()) {
                                TelegramBotForegroundService.start(applicationContext)
                            }
                        }
                        sessionReady = true
                        withContext(Dispatchers.IO) {
                            com.truckerload.data.auth.SilentAuthRestorer.restore(
                                context = applicationContext,
                                authStore = authStore,
                                userProfileStore = userProfileStore,
                            )
                            runCatching {
                                runSessionRepairsIfNeeded(activeUserId, deps.loadRepository)
                            }.onFailure { e ->
                                android.util.Log.w("MainActivity", "Load repair failed", e)
                            }
                            if (!BuildConfig.LOCAL_ONLY_MODE) {
                                com.truckerload.data.backup.DriveSyncWorker.enqueuePeriodic(applicationContext)
                                com.truckerload.sync.OutboundSyncWorker.enqueue(applicationContext)
                                com.truckerload.sync.CloudSyncWorker.enqueuePeriodic(applicationContext)
                                com.truckerload.sync.MediaSyncWorker.enqueue(applicationContext)
                                com.truckerload.sync.MediaSyncWorker.enqueuePeriodic(applicationContext)
                                ServerTelegramInboxWorker.enqueue(applicationContext)
                                ServerTelegramInboxWorker.enqueuePeriodic(applicationContext)
                                PushTokenRegistrationWorker.enqueue(applicationContext)
                                runCatching {
                                    com.truckerload.data.sync.CloudSyncEngine.onSessionReady(applicationContext)
                                }.onFailure { e ->
                                    android.util.Log.w("MainActivity", "Cloud sync on session ready failed", e)
                                }
                                runCatching {
                                    com.truckerload.data.backup.GoogleDriveBackupService
                                        .pushAutoBackupIfEnabled(applicationContext)
                                }
                            }
                        }
                    } else {
                        sessionReady = true
                    }
                } else {
                    SessionTeardown.beforeLogout(applicationContext)
                    userComponentManager.endSession()
                    session = null
                    sessionReady = true
                }
            }

            val themeMode by settingsDataStore.themeMode.collectAsStateWithLifecycle(
                initialValue = AppThemeMode.SYSTEM,
            )
            val oledDark by settingsDataStore.oledDark.collectAsStateWithLifecycle(
                initialValue = false,
            )
            val reduceMotionPref by settingsDataStore.reduceMotion.collectAsStateWithLifecycle(
                initialValue = false,
            )
            val darkTheme = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            TruckerLoadTheme(
                darkTheme = darkTheme,
                themeMode = themeMode,
                oledDark = oledDark,
                reduceMotion = reduceMotionPref,
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        !sessionReady -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        !isLoggedIn || session == null -> {
                            CompositionLocalProvider(
                                LocalSettingsDataStore provides settingsDataStore,
                                LocalAuthStore provides authStore,
                                LocalAuthCredentialsStore provides authCredentialsStore,
                                LocalUserProfileStore provides userProfileStore,
                            ) {
                                AuthNavHost(
                                    authStore = authStore,
                                    onLoginSuccess = { /* session LaunchedEffect rebuilds deps */ },
                                )
                            }
                        }
                        else -> {
                            val deps = session
                            if (deps != null) {
                                val biometricStore = remember {
                                    com.truckerload.data.preferences.BiometricUnlockStore(applicationContext)
                                }
                                val gateEnabled = authStore.authProvider() ==
                                    AuthProvider.EMAIL &&
                                    biometricStore.isEnabled()
                                // Locals remain for screens that still collect Flows outside ViewModels.
                                CompositionLocalProvider(
                                    LocalSettingsDataStore provides settingsDataStore,
                                    LocalAuthStore provides authStore,
                                    LocalAuthCredentialsStore provides authCredentialsStore,
                                    LocalUserProfileStore provides userProfileStore,
                                    LocalLoadRepository provides deps.loadRepository,
                                    LocalPaycheckRepository provides deps.paycheckRepository,
                                    LocalDieselRepository provides deps.dieselRepository,
                                    LocalWeekRepository provides deps.weekRepository,
                                    LocalAiRepository provides deps.aiRepository,
                                    LocalRpmThresholdsStore provides deps.rpmThresholdsStore,
                                    LocalSelectedStateStore provides deps.selectedStateStore,
                                    LocalStatsSelectionStore provides deps.statsSelectionStore,
                                    LocalWeeklyProfitGoalStore provides deps.weeklyProfitGoalStore,
                                    LocalLastUsedDefaultsStore provides deps.lastUsedDefaultsStore,
                                    LocalAnalyticsRepository provides deps.analyticsRepository,
                                    LocalPhotoRepository provides deps.photoRepository,
                                    LocalScanRepository provides deps.scanRepository,
                                    LocalProfileRepository provides deps.profileRepository,
                                    LocalSocialSyncCoordinator provides deps.socialSyncCoordinator,
                                    LocalVoiceRepository provides deps.voiceRepository,
                                    LocalMaintenanceRepository provides deps.maintenanceRepository,
                                ) {
                                    com.truckerload.presentation.auth.BiometricUnlockGate(enabled = gateEnabled) {
                                        // Reset Nav/ViewModel stores when the account changes.
                                        key(deps.userId) {
                                            NavGraph(
                                                deepLinkRoute = deepLinkRoute,
                                                onDeepLinkHandled = { deepLinkRoute = null },
                                            )
                                        }
                                        AutoRestoreDialog(loadRepository = deps.loadRepository)
                                        LegacyAbsorbDialog(
                                            onSessionRebuilt = { rebuilt ->
                                                session = rebuilt
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkRoute(intent.getStringExtra(EXTRA_ROUTE))
    }

    private fun handleDeepLinkRoute(route: String?) {
        deepLinkRoute = route
        if (route == ROUTE_JOURNAL_THIS_WEEK) {
            WidgetDeepLink.markOpenJournalThisWeek(this)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            android.util.Log.i(
                "TruckLog",
                getString(R.string.notification_permission_rationale),
            )
        }
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQ_POST_NOTIFICATIONS,
        )
    }

    /**
     * One-shot per account: date + inflated-miles repairs.
     * Traced; slow runs (>500ms) reported to Crashlytics as slow_session_repair.
     */
    private suspend fun runSessionRepairsIfNeeded(userId: String, loadRepository: LoadRepository) {
        val store = StartupRepairStore(this)
        if (store.isSessionRepairDone(userId)) return
        Trace.beginSection("session_repair")
        val started = SystemClock.elapsedRealtime()
        try {
            loadRepository.repairMislabeledLoadDates()
            loadRepository.repairInflatedLoadedMiles()
            store.markSessionRepairDone(userId)
        } finally {
            Trace.endSection()
            val elapsed = SystemClock.elapsedRealtime() - started
            if (elapsed > 500L) {
                CrashReporting.setCustomKey("slow_session_repair_ms", elapsed)
                CrashReporting.setCustomKey("slow_session_repair_user", userId)
                CrashReporting.recordException(
                    RuntimeException("slow_session_repair elapsedMs=$elapsed"),
                )
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "truckerload.route"
        const val ROUTE_ADD_LOAD = "add_load"
        const val ROUTE_JOURNAL_THIS_WEEK = WidgetDeepLink.ROUTE_JOURNAL_THIS_WEEK
        private const val REQ_POST_NOTIFICATIONS = 1001
    }
}
