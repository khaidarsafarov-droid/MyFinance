package com.truckerload.presentation.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.crowd.CrowdRateRepository
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.domain.crowd.CrowdLaneAggregate
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdScope
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
    val scope: CrowdScope = CrowdScope.ALL,
    val stateSummary: CrowdStateSummary? = null,
    val topLanes: List<CrowdLaneAggregate> = emptyList(),
    val usingCommunitySample: Boolean = false,
    val errorMessage: String? = null,
)

class MapViewModel(
    private val crowdRateRepository: CrowdRateRepository,
    private val selectedStateStore: SelectedStateStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MapUiState(selectedStateCode = selectedStateStore.current()),
    )
    val uiState = _uiState.asStateFlow()

    private var allReports: List<CrowdRateReport> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val reports = withContext(Dispatchers.IO) {
                    crowdRateRepository.loadWeekReports()
                }
                allReports = reports
                applyScope(_uiState.value.scope, _uiState.value.selectedStateCode)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
            }
        }
    }

    fun setScope(scope: CrowdScope) {
        applyScope(scope, _uiState.value.selectedStateCode)
    }

    fun setSelectedState(code: String) {
        selectedStateStore.save(code)
        applyScope(_uiState.value.scope, code)
    }

    private fun applyScope(scope: CrowdScope, selectedCode: String) {
        val scoped = CrowdMapAggregator.filterByScope(allReports, scope)
        val metrics = CrowdMapAggregator.heatmapFromOutbound(scoped)
        val summary = selectedCode.takeIf { it.isNotBlank() }?.let {
            CrowdMapAggregator.stateSummary(scoped, it)
        }
        _uiState.update {
            it.copy(
                metrics = metrics,
                selectedStateCode = selectedCode,
                scope = scope,
                totalReports = scoped.size,
                stateSummary = summary,
                topLanes = CrowdMapAggregator.topLanes(scoped),
                usingCommunitySample = allReports.any { r -> r.id.startsWith("net:") },
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    private fun Throwable.toUiMessage(): String =
        localizedMessage ?: message ?: javaClass.simpleName

    class Factory(
        private val crowdRateRepository: CrowdRateRepository,
        private val selectedStateStore: SelectedStateStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MapViewModel(crowdRateRepository, selectedStateStore) as T
    }
}
