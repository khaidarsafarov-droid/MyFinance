package com.truckerload.presentation.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.LoadRepository
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

data class LoadDetailUiState(
    val isLoading: Boolean = true,
    val load: Load? = null,
    val loadError: String? = null,
    val showFinishPicker: Boolean = false,
)

sealed class LoadDetailEvent {
    data object Deleted : LoadDetailEvent()
    data class Message(val text: String) : LoadDetailEvent()
}

class LoadDetailViewModel(
    application: Application,
    private val loadId: String,
    private val loadRepository: LoadRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoadDetailUiState())
    val uiState: StateFlow<LoadDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoadDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoadDetailEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun setShowFinishPicker(show: Boolean) {
        _uiState.update { it.copy(showFinishPicker = show) }
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
                val loaded = loadRepository.getLoadById(loadId)
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
                val next = current.copy(
                    actualFinishDate = isoDate,
                    updatedAt = System.currentTimeMillis(),
                ).withRouteMetrics()
                loadRepository.updateLoad(next)
                _uiState.update { it.copy(load = next, showFinishPicker = false) }
            } catch (e: Exception) {
                _events.emit(LoadDetailEvent.Message(e.message ?: saveErrorFallback))
            }
        }
    }

    fun updateDispute(updated: Load, saveErrorFallback: String) {
        viewModelScope.launch {
            try {
                loadRepository.updateLoad(updated)
                _uiState.update { it.copy(load = updated) }
            } catch (e: Exception) {
                _events.emit(LoadDetailEvent.Message(e.message ?: saveErrorFallback))
            }
        }
    }

    class Factory(
        private val application: Application,
        private val loadId: String,
        private val loadRepository: LoadRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoadDetailViewModel(application, loadId, loadRepository) as T
    }
}
