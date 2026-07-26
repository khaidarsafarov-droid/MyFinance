package com.truckerload.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.data.repository.ScanRepository
import com.truckerload.data.repository.SocialRepository
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.presentation.components.AutoRestoreDialog
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
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalStatsSelectionStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.di.LocalWeekRepository
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.navigation.AuthNavHost
import com.truckerload.presentation.navigation.NavGraph
import com.truckerload.presentation.theme.ThemeManager
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.sync.ServerTelegramInboxWorker
import com.truckerload.sync.PushTokenRegistrationWorker
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
            var dependencies by remember { mutableStateOf<MainDependencies?>(null) }
            var sessionReady by remember { mutableStateOf(false) }
            val isLoggedIn by authStore.isLoggedIn.collectAsStateWithLifecycle()
            val userId by authStore.userId.collectAsStateWithLifecycle()

            LaunchedEffect(isLoggedIn, userId) {
                sessionReady = false
                if (BuildConfig.LOCAL_ONLY_MODE && (!isLoggedIn || userId.isNullOrBlank())) {
                    authStore.login(
                        userId = AccountIds.LOCAL_DEV,
                        email = "local@device",
                        rememberMe = true,
                    )
                    return@LaunchedEffect
                }
                if (isLoggedIn && !userId.isNullOrBlank()) {
                    val activeUserId = userId as String
                    val deps = createDependencies(
                        context = applicationContext,
                        userId = activeUserId,
                        authStore = authStore,
                        authCredentialsStore = authCredentialsStore,
                        userProfileStore = userProfileStore,
                    )
                    dependencies = deps
                    if (!TelegramSyncMode.isServer()) {
                        val tokenStore = TelegramTokenStore(applicationContext, activeUserId)
                        tokenStore.bootstrapFromBuildConfigIfEmpty()
                        if (tokenStore.hasToken()) {
                            TelegramBotForegroundService.start(applicationContext)
                        }
                    }
                    // Silent session check + background sync. In LOCAL_ONLY_MODE skip
                    // cloud/Drive/push workers so cold start stays responsive on slow devices.
                    withContext(Dispatchers.IO) {
                        com.truckerload.data.auth.SilentAuthRestorer.restore(
                            context = applicationContext,
                            authStore = authStore,
                            userProfileStore = userProfileStore,
                        )
                        runCatching {
                            deps.loadRepository.repairMislabeledLoadDates()
                        }.onFailure { e ->
                            android.util.Log.w("MainActivity", "Load date repair failed", e)
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
                    sessionReady = true
                } else {
                    TelegramBotForegroundService.stopForLogout(applicationContext)
                    kotlinx.coroutines.delay(300)
                    AppDatabase.closeCurrent()
                    userProfileStore.unbind()
                    dependencies = null
                    sessionReady = true
                }
            }

            val themeMode by settingsDataStore.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            LaunchedEffect(themeMode) {
                ThemeManager.apply(themeMode)
            }
            TruckerLoadTheme(darkTheme = darkTheme, themeMode = themeMode) {
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
                        !isLoggedIn || dependencies == null -> {
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
                            val deps = dependencies
                            if (deps != null) {
                            val biometricStore = remember {
                                com.truckerload.data.preferences.BiometricUnlockStore(applicationContext)
                            }
                            val gateEnabled = deps.authStore.authProvider() ==
                                com.truckerload.data.preferences.AuthProvider.EMAIL &&
                                biometricStore.isEnabled()
                            CompositionLocalProvider(
                                LocalSettingsDataStore provides settingsDataStore,
                                LocalAuthStore provides deps.authStore,
                                LocalAuthCredentialsStore provides deps.authCredentialsStore,
                                LocalUserProfileStore provides deps.userProfileStore,
                                LocalLoadRepository provides deps.loadRepository,
                                LocalPaycheckRepository provides deps.paycheckRepository,
                                LocalDieselRepository provides deps.dieselRepository,
                                LocalWeekRepository provides deps.weekRepository,
                                LocalAiRepository provides deps.aiRepository,
                                LocalRpmThresholdsStore provides deps.rpmThresholdsStore,
                                LocalSelectedStateStore provides deps.selectedStateStore,
                                LocalStatsSelectionStore provides deps.statsSelectionStore,
                                LocalWeeklyProfitGoalStore provides deps.weeklyProfitGoalStore,
                                LocalAnalyticsRepository provides deps.analyticsRepository,
                                LocalPhotoRepository provides deps.photoRepository,
                                LocalScanRepository provides deps.scanRepository,
                                LocalSocialRepository provides deps.socialRepository,
                                LocalVoiceRepository provides deps.voiceRepository,
                                LocalMaintenanceRepository provides deps.maintenanceRepository,
                            ) {
                                com.truckerload.presentation.auth.BiometricUnlockGate(enabled = gateEnabled) {
                                    NavGraph(
                                        deepLinkRoute = deepLinkRoute,
                                        onDeepLinkHandled = { deepLinkRoute = null },
                                    )
                                    AutoRestoreDialog(loadRepository = deps.loadRepository)
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

    private data class MainDependencies(
        val authStore: AuthStore,
        val authCredentialsStore: AuthCredentialsStore,
        val userProfileStore: UserProfileStore,
        val loadRepository: LoadRepository,
        val paycheckRepository: PaycheckRepository,
        val dieselRepository: DieselRepository,
        val weekRepository: WeekRepository,
        val rpmThresholdsStore: RpmThresholdsStore,
        val selectedStateStore: SelectedStateStore,
        val statsSelectionStore: StatsSelectionStore,
        val weeklyProfitGoalStore: WeeklyProfitGoalStore,
        val analyticsRepository: AnalyticsRepository,
        val photoRepository: PhotoRepository,
        val scanRepository: ScanRepository,
        val socialRepository: SocialRepository,
        val voiceRepository: VoiceRepository,
        val aiRepository: AiRepository,
        val maintenanceRepository: MaintenanceRepository,
    )

    private suspend fun createDependencies(
        context: Context,
        userId: String,
        authStore: AuthStore,
        authCredentialsStore: AuthCredentialsStore,
        userProfileStore: UserProfileStore,
    ): MainDependencies = withContext(Dispatchers.IO) {
        userProfileStore.bindUser(userId)
        val db = AppDatabase.getInstance(context, userId)
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)
        val weekRepository = WeekRepository(loadRepository, paycheckRepository, dieselRepository)
        MainDependencies(
            authStore = authStore,
            authCredentialsStore = authCredentialsStore,
            userProfileStore = userProfileStore,
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
            weekRepository = weekRepository,
            rpmThresholdsStore = RpmThresholdsStore(context, userId),
            selectedStateStore = SelectedStateStore(context, userId),
            statsSelectionStore = StatsSelectionStore(context, userId),
            weeklyProfitGoalStore = WeeklyProfitGoalStore(context, userId),
            analyticsRepository = AnalyticsRepository(db),
            photoRepository = PhotoRepository(db),
            scanRepository = ScanRepository(db),
            socialRepository = SocialRepository(db, loadRepository, userProfileStore, context),
            voiceRepository = VoiceRepository(db, context),
            aiRepository = AiRepository(),
            maintenanceRepository = MaintenanceRepository(db),
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        // Rationale string is available for UI that explains why we need it (API 33+).
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

    companion object {
        const val EXTRA_ROUTE = "truckerload.route"
        const val ROUTE_ADD_LOAD = "add_load"
        const val ROUTE_JOURNAL_THIS_WEEK = WidgetDeepLink.ROUTE_JOURNAL_THIS_WEEK
        private const val REQ_POST_NOTIFICATIONS = 1001
    }
}
