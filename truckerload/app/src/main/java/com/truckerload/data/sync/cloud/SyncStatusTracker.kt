package com.truckerload.data.sync.cloud

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SyncStatusTracker @Inject constructor() {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    fun markIdle() {
        _state.value = SyncState.Idle
    }

    fun markSyncing() {
        _state.value = SyncState.Syncing
    }

    fun markError(message: String, retryable: Boolean = true) {
        _state.value = SyncState.Error(message = message, retryable = retryable)
    }
}
