package com.truckerload.presentation.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.ActualFinishDate
import com.truckerload.domain.model.DisputePayout
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.withRouteMetrics
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LoadDetailUiState(
    val isLoading: Boolean = true,
    val load: Load? = null,
    val loadError: String? = null,
)

sealed class LoadDetailEvent {
    data object Deleted : LoadDetailEvent()
    data class Message(val text: String) : LoadDetailEvent()
}

@HiltViewModel
class LoadDetailViewModel @Inject constructor(
    application: Application,
    private val loadRepository: LoadRepository,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val loadId = Uri.decode(savedStateHandle.get<String>("loadId").orEmpty())

    private val _uiState = MutableStateFlow(LoadDetailUiState())
    val uiState: StateFlow<LoadDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoadDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoadDetailEvent> = _events.asSharedFlow()
    private val disputePersistMutex = Mutex()

    init {
        refresh()
    }

    fun refresh() {
        val app = getApplication<Application>()
        if (loadId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    loadError = app.getString(R.string.load_invalid),
                    load = null,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            try {
                // Always recompute duration/pace from stops + actualFinishDate so the
                // detail stats stay in sync even if denormalized DB fields were stale.
                val loaded = loadRepository.getLoadById(loadId)?.withRouteMetrics()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        load = loaded,
                        loadError = if (loaded == null) {
                            app.getString(R.string.load_detail_not_found)
                        } else {
                            null
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = e.message ?: app.getString(R.string.load_error_loading),
                        load = null,
                    )
                }
            }
        }
    }

    fun delete(fallbackMessage: String) {
        viewModelScope.launch {
            try {
                loadRepository.deleteLoad(loadId)
                _events.emit(LoadDetailEvent.Deleted)
            } catch (e: Exception) {
                _events.emit(LoadDetailEvent.Message(e.message ?: fallbackMessage))
            }
        }
    }

    fun setActualFinishDate(isoDate: String?, saveErrorFallback: String) {
        val current = _uiState.value.load ?: return
        viewModelScope.launch {
            try {
                val normalizedDate = ActualFinishDate.normalize(isoDate)
                val next = current.copy(
                    actualFinishDate = normalizedDate,
                    updatedAt = System.currentTimeMillis(),
                ).withRouteMetrics()
                loadRepository.updateLoad(next)
                // Reload from Room so UI reflects persisted durationDays/pace/lastDelMillis.
                val reloaded = loadRepository.getLoadById(loadId)?.withRouteMetrics() ?: next
                _uiState.update { it.copy(load = reloaded) }
            } catch (e: Exception) {
                _events.emit(LoadDetailEvent.Message(e.message ?: saveErrorFallback))
            }
        }
    }

    fun updateDispute(updated: Load, saveErrorFallback: String) {
        viewModelScope.launch {
            disputePersistMutex.withLock {
                try {
                    val previous = loadRepository.getLoadById(loadId)
                        ?: _uiState.value.load
                        ?: updated
                    val merged = DisputePayout.mergeIncoming(previous, updated)
                    val settled = DisputePayout.settleFrom(previous, merged).withRouteMetrics()
                    loadRepository.updateLoad(settled)
                    val reloaded = loadRepository.getLoadById(loadId)?.withRouteMetrics() ?: settled
                    _uiState.update { it.copy(load = reloaded) }
                } catch (e: Exception) {
                    _events.emit(LoadDetailEvent.Message(e.message ?: saveErrorFallback))
                }
            }
        }
    }
}
