package com.truckerload.presentation.di

import androidx.lifecycle.ViewModel
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Weekly profit-goal amount for home and tablet dashboards. */
@HiltViewModel
class WeeklyProfitGoalViewModel @Inject constructor(store: WeeklyProfitGoalStore) : ViewModel() {
    val goalAmount: StateFlow<Double> = store.goalAmount
}
