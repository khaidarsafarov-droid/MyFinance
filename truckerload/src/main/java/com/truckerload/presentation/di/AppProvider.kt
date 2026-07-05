package com.truckerload.presentation.di

import androidx.compose.runtime.compositionLocalOf
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.WeekRepository

val LocalLoadRepository = compositionLocalOf<LoadRepository> { error("No LoadRepository provided") }
val LocalAiRepository = compositionLocalOf<AiRepository?> { null }
val LocalPaycheckRepository = compositionLocalOf<PaycheckRepository> { error("No PaycheckRepository provided") }
val LocalDieselRepository = compositionLocalOf<DieselRepository> { error("No DieselRepository provided") }
val LocalWeekRepository = compositionLocalOf<WeekRepository> { error("No WeekRepository provided") }
val LocalRpmThresholdsStore = compositionLocalOf<RpmThresholdsStore> { error("No RpmThresholdsStore provided") }
val LocalSelectedStateStore = compositionLocalOf<SelectedStateStore> { error("No SelectedStateStore provided") }
val LocalStatsSelectionStore = compositionLocalOf<StatsSelectionStore> { error("No StatsSelectionStore provided") }
