package com.truckerload.presentation.screens.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.sync.cloud.SyncModeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val syncModeStore: SyncModeStore,
) : ViewModel() {

    /** True when loads may also live in the user's own cloud backup. */
    val cloudBackupEnabled: StateFlow<Boolean> = syncModeStore.mode
        .map { syncModeStore.allowsCloudCalls() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = syncModeStore.allowsCloudCalls(),
        )
}
