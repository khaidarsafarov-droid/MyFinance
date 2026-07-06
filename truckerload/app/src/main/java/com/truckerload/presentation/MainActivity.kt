package com.truckerload.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.utils.FeedbackManager
import com.truckerload.BuildConfig
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.data.repository.ScanRepository
import com.truckerload.data.repository.SocialRepository
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.presentation.di.LocalAuthCredentialsStore
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.di.LocalSelectedStateStore
import com.truckerload.presentation.di.LocalStatsSelectionStore
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.di.LocalWeekRepository
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.data.repository.WeekRepository
import com.truckerload.presentation.di.LocalAnalyticsRepository
import com.truckerload.presentation.di.LocalPhotoRepository
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalVoiceRepository
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.components.AutoRestoreDialog
import com.truckerload.presentation.navigation.NavGraph
import com.truckerload.presentation.theme.ThemeManager
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetDeepLink
import com.truckerload.utils.AppLocale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
        val db = AppDatabase.getInstance(applicationContext)
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)
        val weekRepository = WeekRepository(loadRepository, paycheckRepository, dieselRepository)
        val authStore = AuthStore(applicationContext)
        val authCredentialsStore = AuthCredentialsStore(applicationContext)
        val userProfileStore = UserProfileStore(applicationContext)
        if (BuildConfig.LOCAL_ONLY_MODE) {
            authStore.login(rememberMe = true)
        }
        val rpmThresholdsStore = RpmThresholdsStore(applicationContext)
        val selectedStateStore = SelectedStateStore(applicationContext)
        val statsSelectionStore = StatsSelectionStore(applicationContext)
        val weeklyProfitGoalStore = WeeklyProfitGoalStore(applicationContext)
        val analyticsRepository = AnalyticsRepository(db)
        val photoRepository = PhotoRepository(db)
        val scanRepository = ScanRepository(db)
        val socialRepository = SocialRepository(db, loadRepository, userProfileStore, applicationContext)
        val voiceRepository = VoiceRepository(db, applicationContext)
        val aiRepository = AiRepository()
        val settingsDataStore = SettingsDataStore(applicationContext)
        setContent {
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
                    CompositionLocalProvider(
                        LocalSettingsDataStore provides settingsDataStore,
                        LocalAuthStore provides authStore,
                        LocalAuthCredentialsStore provides authCredentialsStore,
                        LocalUserProfileStore provides userProfileStore,
                        LocalLoadRepository provides loadRepository,
                        LocalPaycheckRepository provides paycheckRepository,
                        LocalDieselRepository provides dieselRepository,
                        LocalWeekRepository provides weekRepository,
                        LocalAiRepository provides aiRepository,
                        LocalRpmThresholdsStore provides rpmThresholdsStore,
                        LocalSelectedStateStore provides selectedStateStore,
                        LocalStatsSelectionStore provides statsSelectionStore,
                        LocalWeeklyProfitGoalStore provides weeklyProfitGoalStore,
                        LocalAnalyticsRepository provides analyticsRepository,
                        LocalPhotoRepository provides photoRepository,
                        LocalScanRepository provides scanRepository,
                        LocalSocialRepository provides socialRepository,
                        LocalVoiceRepository provides voiceRepository,
                    ) {
                        NavGraph(
                            deepLinkRoute = deepLinkRoute,
                            onDeepLinkHandled = { deepLinkRoute = null }
                        )
                        AutoRestoreDialog(loadRepository = loadRepository)
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

    companion object {
        const val EXTRA_ROUTE = "truckerload.route"
        const val ROUTE_ADD_LOAD = "add_load"
        const val ROUTE_JOURNAL_THIS_WEEK = WidgetDeepLink.ROUTE_JOURNAL_THIS_WEEK
    }
}
