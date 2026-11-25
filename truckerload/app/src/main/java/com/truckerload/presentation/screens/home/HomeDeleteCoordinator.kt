package com.truckerload.presentation.screens.home

import android.app.Application
import com.truckerload.R
import com.truckerload.data.local.DeletedLoadLedger
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.runReportedSuspending
import com.truckerload.widget.WidgetDataUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Soft delete with an Undo window, then hard delete. Owns every delete-related flag the Home
 * journal shows: ids hidden from the list, the Undo target, the last error, and the swipe reset.
 *
 * [scope] is the view model scope. [commitScope] outlives it so a pending delete can still finish
 * from `onCleared`, where the view model scope is already cancelled.
 */
internal class HomeDeleteCoordinator(
    private val app: Application,
    private val loadRepository: LoadRepository,
    private val scope: CoroutineScope,
    private val commitScope: CoroutineScope,
    private val optimisticOverlay: MutableStateFlow<Map<String, Load>>,
    private val tripIdOf: (String) -> String?,
) {
    /** IDs being deleted: excluded from the merged list so they cannot reappear via the overlay. */
    private val _pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingDeleteIds: StateFlow<Set<String>> = _pendingDeleteIds.asStateFlow()

    private val _pendingDeleteConfirmId = MutableStateFlow<String?>(null)
    val pendingDeleteConfirmId: StateFlow<String?> = _pendingDeleteConfirmId.asStateFlow()

    /** Load id waiting for the Undo snackbar before the hard delete. */
    private val _undoDeleteLoadId = MutableStateFlow<String?>(null)
    val undoDeleteLoadId: StateFlow<String?> = _undoDeleteLoadId.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    /** Bumped when a delete is undone or fails so swipe cards snap back. */
    private val _swipeSettleGeneration = MutableStateFlow(0)
    val swipeSettleGeneration: StateFlow<Int> = _swipeSettleGeneration.asStateFlow()

    private var undoDeleteJob: Job? = null

    /** Room confirmed these ids exist, so their delete markers are no longer needed. */
    fun onRoomLoads(ids: Set<String>) {
        _pendingDeleteIds.update { pending -> pending.filter { it !in ids }.toSet() }
    }

    /**
     * Soft-hides the load immediately and starts an Undo window before the hard delete.
     * Keeps the confirm APIs for callers that still use the dialog path.
     */
    fun request(loadId: String) {
        if (loadId.isBlank()) return
        FeedbackManager.onDeleteGesture()
        val previous = _undoDeleteLoadId.value
        if (previous != null && previous != loadId) {
            commit(previous)
        }
        undoDeleteJob?.cancel()
        val tripId = tripIdOf(loadId)
        optimisticOverlay.update { it - loadId }
        _pendingDeleteIds.update { it + loadId }
        _undoDeleteLoadId.value = loadId
        DeletedLoadLedger.markPending(app, loadId, tripId)
        undoDeleteJob = scope.launch {
            delay(UNDO_DELETE_WINDOW_MS)
            commit(loadId)
        }
    }

    fun undo() {
        val loadId = _undoDeleteLoadId.value ?: return
        undoDeleteJob?.cancel()
        undoDeleteJob = null
        _undoDeleteLoadId.value = null
        _pendingDeleteIds.update { it - loadId }
        DeletedLoadLedger.cancelPending(app, loadId)
        _swipeSettleGeneration.update { it + 1 }
    }

    fun dismiss() {
        _pendingDeleteConfirmId.value = null
        undo()
    }

    fun clearError() {
        _deleteError.value = null
    }

    fun confirm() {
        val loadId = _pendingDeleteConfirmId.value ?: _undoDeleteLoadId.value ?: return
        _pendingDeleteConfirmId.value = null
        commit(loadId)
    }

    private fun commit(loadId: String) {
        if (loadId.isBlank()) return
        undoDeleteJob?.cancel()
        undoDeleteJob = null
        if (_undoDeleteLoadId.value == loadId) {
            _undoDeleteLoadId.value = null
        }
        scope.launch {
            optimisticOverlay.update { it - loadId }
            _pendingDeleteIds.update { it + loadId }
            runReportedSuspending(
                block = {
                    loadRepository.deleteLoad(loadId)
                    WidgetDataUpdater.updateWidgetData(app.applicationContext)
                    _pendingDeleteIds.update { it - loadId }
                },
                onFailure = { e ->
                    _pendingDeleteIds.update { it - loadId }
                    _swipeSettleGeneration.update { it + 1 }
                    _deleteError.value = failureMessage(e)
                },
            )
        }
    }

    /** Leaving Home / process death: hard-delete so the load cannot come back. */
    fun commitAllNow() {
        val pending = (_pendingDeleteIds.value + listOfNotNull(_undoDeleteLoadId.value))
            .filter { it.isNotBlank() }
            .toSet()
        if (pending.isEmpty()) return
        undoDeleteJob?.cancel()
        undoDeleteJob = null
        _undoDeleteLoadId.value = null
        commitScope.launch {
            val failed = mutableListOf<String>()
            for (id in pending) {
                runReportedSuspending(block = { loadRepository.deleteLoad(id) }) { e ->
                    failed += id
                    _deleteError.value = failureMessage(e)
                }
            }
            _pendingDeleteIds.update { (it - pending) + failed }
            if (failed.isEmpty()) {
                WidgetDataUpdater.updateWidgetData(app.applicationContext)
            }
        }
    }

    private fun failureMessage(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: app.getString(R.string.home_delete_failed)

    private companion object {
        const val UNDO_DELETE_WINDOW_MS = 5_000L
    }
}
