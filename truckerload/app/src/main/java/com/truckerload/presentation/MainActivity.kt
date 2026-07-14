package com.truckerload.presentation

import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.truckerload.BuildConfig
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
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
import com.truckerload.presentation.navigation.NavGraph
import com.truckerload.presentation.theme.ThemeManager
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.utils.AppLocale
import com.truckerload.utils.FeedbackManager
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetDeepLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    private var deepLinkRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkRoute(intent.getStringExtra(EXTRA_ROUTE))
        WidgetDataUpdater.updateWidgetData(applicationContext)
        val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                android.util.Log.e(
                    "TruckLog",
                    "Uncaught exception in ${t.name}: ${e.javaClass.name}: ${e.message}",
                )
            } catch (_: Throwable) {
                android.util.Log.e("TruckLog", "Uncaught exception in ${t.name}")
            }
            defaultUncaughtExceptionHandler?.uncaughtException(t, e)
        }
        enableEdgeToEdge()
        FeedbackManager.init(applicationContext)
        val settingsDataStore = SettingsDataStore(applicationContext)
        setContent {
            var dependencies by remember { mutableStateOf<MainDependencies?>(null) }
            LaunchedEffect(Unit) {
                dependencies = createDependencies(applicationContext)
            }

            val themeMode by settingsDataStore.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
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
                    val deps = dependencies
                    if (deps == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
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
                        ) {
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
    )

    private suspend fun createDependencies(context: Context): MainDependencies = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)
        val weekRepository = WeekRepository(loadRepository, paycheckRepository, dieselRepository)
        val authStore = AuthStore(context)
        val authCredentialsStore = AuthCredentialsStore(context)
        val userProfileStore = UserProfileStore(context)
        if (BuildConfig.LOCAL_ONLY_MODE) {
            authStore.login(rememberMe = true)
        }
        MainDependencies(
            authStore = authStore,
            authCredentialsStore = authCredentialsStore,
            userProfileStore = userProfileStore,
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
            weekRepository = weekRepository,
            rpmThresholdsStore = RpmThresholdsStore(context),
            selectedStateStore = SelectedStateStore(context),
            statsSelectionStore = StatsSelectionStore(context),
            weeklyProfitGoalStore = WeeklyProfitGoalStore(context),
            analyticsRepository = AnalyticsRepository(db),
            photoRepository = PhotoRepository(db),
            scanRepository = ScanRepository(db),
            socialRepository = SocialRepository(db, loadRepository, userProfileStore, context),
            voiceRepository = VoiceRepository(db, context),
            aiRepository = AiRepository(),
        )
    }

    companion object {
        const val EXTRA_ROUTE = "truckerload.route"
        const val ROUTE_ADD_LOAD = "add_load"
        const val ROUTE_JOURNAL_THIS_WEEK = WidgetDeepLink.ROUTE_JOURNAL_THIS_WEEK
    }
}
