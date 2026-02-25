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
import com.truckerload.data.remote.GeminiService
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.GeminiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalGeminiRepository
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
        val geminiApiKey = com.truckerload.BuildConfig.GEMINI_API_KEY
        val geminiRepository = if (geminiApiKey.isNotEmpty()) {
            GeminiRepository(GeminiService(geminiApiKey))
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
                        LocalGeminiRepository provides geminiRepository
                    ) {
                        NavGraph()
                    }
                }
            }
        }
    }
}
