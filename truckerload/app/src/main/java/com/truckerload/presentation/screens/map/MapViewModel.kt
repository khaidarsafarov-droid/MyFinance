package com.truckerload.presentation.screens.map

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.crowd.CrowdLaneAggregate
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdStateSummary
import com.truckerload.presentation.components.USStateMetric
import com.truckerload.utils.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MapUiState(
    val metrics: List<USStateMetric> = emptyList(),
    val selectedStateCode: String = "",
    val period: MapPeriod = MapPeriod.WEEK,
    val isLoading: Boolean = true,
    val totalReports: Int = 0,
    val stateSummary: CrowdStateSummary? = null,
    val topLanes: List<CrowdLaneAggregate> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val loadRepository: LoadRepository,
    private val selectedStateStore: SelectedStateStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MapUiState(selectedStateCode = selectedStateStore.current()),
    )
    val uiState = _uiState.asStateFlow()

    private var allLoadsReports: List<CrowdRateReport> = emptyList()
    private var refreshJob: Job? = null
    private var refreshGeneration = 0

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatchingCancellable {
                val loads = withContext(Dispatchers.IO) { loadRepository.getAllLoadsOnce() }
                // Keep a wide cache, then slice by the selected period in applySelection.
                allLoadsReports = withContext(Dispatchers.Default) {
                    CrowdMapAggregator.reportsFromLoads(
                        loads,
                        windowMs = MapPeriod.YEAR.windowMs,
                    )
                }
                if (generation != refreshGeneration) return@runCatchingCancellable
                applySelection(_uiState.value.selectedStateCode, _uiState.value.period)
            }.onFailure { error ->
                if (generation != refreshGeneration) return@onFailure
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
            }
        }
    }

    fun setSelectedState(code: String) {
        selectedStateStore.save(code)
        applySelection(code, _uiState.value.period)
    }

    fun setPeriod(period: MapPeriod) {
        if (period == _uiState.value.period) return
        if (refreshJob?.isActive == true) {
            _uiState.update { it.copy(period = period) }
            return
        }
        applySelection(_uiState.value.selectedStateCode, period)
    }

    private fun applySelection(selectedCode: String, period: MapPeriod) {
        val now = System.currentTimeMillis()
        val cutoff = now - period.windowMs
        val reports = allLoadsReports.filter { it.reportedAtMillis >= cutoff }
        val metrics = CrowdMapAggregator.heatmapFromOutbound(reports)
        val summary = selectedCode.takeIf { it.isNotBlank() }?.let {
            CrowdMapAggregator.stateSummary(reports, it)
        }
        _uiState.update {
            it.copy(
                metrics = metrics,
                selectedStateCode = selectedCode,
                period = period,
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

}
