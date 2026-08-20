package com.truckerload.presentation.screens.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.sync.cloud.SyncModeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val syncModeStore: SyncModeStore,
) : ViewModel() {

    val crowdStatsOptIn: StateFlow<Boolean> = settingsDataStore.crowdStatsOptIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    /** True when loads may also live in the user's own cloud backup (not Crowd RPM). */
    val cloudBackupEnabled: StateFlow<Boolean> = syncModeStore.mode
        .map { syncModeStore.allowsCloudCalls() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = syncModeStore.allowsCloudCalls(),
        )

    fun setCrowdStatsOptIn(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveCrowdStatsOptIn(enabled)
        }
    }
}
