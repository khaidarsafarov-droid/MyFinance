package com.truckerload.presentation.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.crowd.CrowdLaneAggregate
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdStateSummary
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
    val totalReports: Int = 0,
    val stateSummary: CrowdStateSummary? = null,
    val topLanes: List<CrowdLaneAggregate> = emptyList(),
    val errorMessage: String? = null,
)

class MapViewModel(
    private val loadRepository: LoadRepository,
    private val selectedStateStore: SelectedStateStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MapUiState(selectedStateCode = selectedStateStore.current()),
    )
    val uiState = _uiState.asStateFlow()

    private var reports: List<CrowdRateReport> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val loads = withContext(Dispatchers.IO) { loadRepository.getAllLoadsOnce() }
                reports = withContext(Dispatchers.Default) {
                    CrowdMapAggregator.reportsFromLoads(loads)
                }
                applySelection(_uiState.value.selectedStateCode)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
            }
        }
    }

    fun setSelectedState(code: String) {
        selectedStateStore.save(code)
        applySelection(code)
    }

    private fun applySelection(selectedCode: String) {
        val metrics = CrowdMapAggregator.heatmapFromOutbound(reports)
        val summary = selectedCode.takeIf { it.isNotBlank() }?.let {
            CrowdMapAggregator.stateSummary(reports, it)
        }
        _uiState.update {
            it.copy(
                metrics = metrics,
                selectedStateCode = selectedCode,
                totalReports = reports.size,
                stateSummary = summary,
                topLanes = CrowdMapAggregator.topLanes(reports),
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    private fun Throwable.toUiMessage(): String =
        localizedMessage ?: message ?: javaClass.simpleName

    class Factory(
        private val loadRepository: LoadRepository,
        private val selectedStateStore: SelectedStateStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MapViewModel(loadRepository, selectedStateStore) as T
    }
}
