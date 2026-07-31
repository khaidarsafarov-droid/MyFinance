package com.truckerload.data.sync.cloud

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : SyncState
}
