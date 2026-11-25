package com.truckerload.presentation.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.local.entities.LoadStatsAgg
import com.truckerload.data.repository.LoadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** All-time journal size for the newcomer checklist on Home. */
@HiltViewModel
class HomeJournalStatsViewModel @Inject constructor(loadRepository: LoadRepository) : ViewModel() {
    val totalStats: StateFlow<LoadStatsAgg> = loadRepository.watchTotalLoadStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadStatsAgg(0, 0.0, 0L))
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
