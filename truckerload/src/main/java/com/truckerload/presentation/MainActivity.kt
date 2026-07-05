package com.truckerload.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.remote.AiService
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.di.LocalSelectedStateStore
import com.truckerload.presentation.di.LocalStatsSelectionStore
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.di.LocalWeekRepository
import com.truckerload.presentation.navigation.NavGraph
import com.truckerload.presentation.theme.TruckerLoadTheme
import androidx.compose.runtime.CompositionLocalProvider

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.getInstance(applicationContext)
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)
        val weekRepository = WeekRepository(loadRepository, paycheckRepository, dieselRepository)
        val rpmThresholdsStore = RpmThresholdsStore(applicationContext)
        val selectedStateStore = SelectedStateStore(applicationContext)
        val statsSelectionStore = StatsSelectionStore(applicationContext)
        val cerebrasApiKey = com.truckerload.BuildConfig.CEREBRAS_API_KEY
        val cerebrasModel = com.truckerload.BuildConfig.CEREBRAS_MODEL
        val aiRepository = if (cerebrasApiKey.isNotEmpty()) {
            AiRepository(AiService(cerebrasApiKey, cerebrasModel, applicationContext))
        } else {
            null
        }
        setContent {
            TruckerLoadTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CompositionLocalProvider(
                        LocalLoadRepository provides loadRepository,
                        LocalPaycheckRepository provides paycheckRepository,
                        LocalDieselRepository provides dieselRepository,
                        LocalWeekRepository provides weekRepository,
                        LocalAiRepository provides aiRepository,
                        LocalRpmThresholdsStore provides rpmThresholdsStore,
                        LocalSelectedStateStore provides selectedStateStore,
                        LocalStatsSelectionStore provides statsSelectionStore
                    ) {
                        NavGraph()
                    }
                }
            }
        }
    }
}
