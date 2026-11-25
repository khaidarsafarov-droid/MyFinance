package com.truckerload.presentation.screens.settings

import androidx.lifecycle.ViewModel
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.data.preferences.RpmThresholdsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Red/green rate-per-distance thresholds shared by the settings, profile and RPM-form screens. */
@HiltViewModel
class RpmThresholdsViewModel @Inject constructor(private val store: RpmThresholdsStore) : ViewModel() {
    val thresholds: StateFlow<RpmThresholds> = store.thresholds

    fun save(minProfit: Double, targetProfit: Double): Result<Unit> = store.save(minProfit, targetProfit)
}
