package com.truckerload.presentation.di

import androidx.compose.runtime.compositionLocalOf
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.GeminiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.WeekRepository

val LocalLoadRepository = compositionLocalOf<LoadRepository> { error("No LoadRepository provided") }
val LocalGeminiRepository = compositionLocalOf<GeminiRepository?> { null }
val LocalPaycheckRepository = compositionLocalOf<PaycheckRepository> { error("No PaycheckRepository provided") }
val LocalDieselRepository = compositionLocalOf<DieselRepository> { error("No DieselRepository provided") }
val LocalWeekRepository = compositionLocalOf<WeekRepository> { error("No WeekRepository provided") }
