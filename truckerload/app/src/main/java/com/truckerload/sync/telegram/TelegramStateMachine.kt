package com.truckerload.sync.telegram

import java.util.concurrent.atomic.AtomicReference

/** Poll-cycle states for [com.truckerload.sync.TelegramBotSyncEngine]. */
sealed interface TelegramSyncState {
    data object Idle : TelegramSyncState
    data object Polling : TelegramSyncState
    data object Syncing : TelegramSyncState
    data class Error(val message: String?) : TelegramSyncState
}

/**
 * Idle → Polling → Syncing → Idle, or any active state → Error → Idle.
 *
 * Pure in-memory marker for diagnostics / future UI; the engine still drives control flow.
 */
class TelegramStateMachine {
    private val state = AtomicReference<TelegramSyncState>(TelegramSyncState.Idle)

    fun current(): TelegramSyncState = state.get()

    fun beginPoll(): TelegramSyncState = transition(TelegramSyncState.Polling)

    fun beginSync(): TelegramSyncState = transition(TelegramSyncState.Syncing)

    fun fail(message: String?): TelegramSyncState =
        transition(TelegramSyncState.Error(message))

    fun idle(): TelegramSyncState = transition(TelegramSyncState.Idle)

    private fun transition(next: TelegramSyncState): TelegramSyncState {
        state.set(next)
        return next
    }
}
