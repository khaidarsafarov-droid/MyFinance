package com.truckerload.presentation.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.presentation.components.USStateMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MapUiState(
    val metrics: List<USStateMetric> = emptyList(),
    val selectedStateCode: String = "",
    val isLoading: Boolean = true,
    val totalLoads: Int = 0,
    val errorMessage: String? = null,
)

class MapViewModel(
    private val loadRepository: LoadRepository,
    private val selectedStateStore: SelectedStateStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MapUiState(selectedStateCode = selectedStateStore.current())
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadStateMetrics()
    }

    fun loadStateMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val loads = loadRepository.getAllLoadsOnce()
                val metrics = withContext(Dispatchers.Default) {
                    MapStateMetrics.computeFromLoads(loads)
                }
                _uiState.update {
                    it.copy(
                        metrics = metrics,
                        totalLoads = loads.size,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
            }
        }
    }

    fun setSelectedState(code: String) {
        selectedStateStore.save(code)
        _uiState.update { it.copy(selectedStateCode = code) }
    }

    private fun Throwable.toUiMessage(): String =
        localizedMessage ?: message ?: javaClass.simpleName

    class Factory(
        private val loadRepository: LoadRepository,
        private val selectedStateStore: SelectedStateStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MapViewModel(loadRepository, selectedStateStore) as T
    }
}
