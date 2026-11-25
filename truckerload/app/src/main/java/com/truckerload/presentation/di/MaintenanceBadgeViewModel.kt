package com.truckerload.presentation.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.MaintenanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Overdue-maintenance count for the phone bar and tablet rail badges. */
@HiltViewModel
class MaintenanceBadgeViewModel @Inject constructor(repository: MaintenanceRepository) : ViewModel() {
    val overdueCount: StateFlow<Int> = repository.watchActiveProgress()
        .map { items -> items.count { it.isDue } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
