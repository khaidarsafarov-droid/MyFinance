package com.truckerload.presentation.screens.analytics

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.domain.model.analytics.AnalyticsSummary
import com.truckerload.domain.model.analytics.DailyData
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.domain.model.analytics.WeekData
import com.truckerload.utils.AnalyticsExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val period: AnalyticsPeriod = AnalyticsPeriod.LAST_12_WEEKS,
    val weeks: List<WeekData> = emptyList(),
    val routes: List<RouteData> = emptyList(),
    val daily: List<DailyData> = emptyList(),
    val summary: AnalyticsSummary? = null,
    val selectedWeekIndex: Int? = null,
    val selectedWeekLoads: List<Load> = emptyList(),
    val exportPath: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository,
    private val app: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var lastDashboard: AnalyticsDashboard? = null

    init {
        refresh()
    }

    fun setPeriod(period: AnalyticsPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period) }
        refresh()
    }

    fun selectWeek(index: Int) {
        val week = _uiState.value.weeks.getOrNull(index) ?: return
        _uiState.update { it.copy(selectedWeekIndex = index) }
        viewModelScope.launch {
            runCatching {
                repository.getLoadsForWeek(week.weekNumber, week.year)
            }.onSuccess { loads ->
                _uiState.update { it.copy(selectedWeekLoads = loads) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.loadDashboard(_uiState.value.period)
            }.onSuccess { dashboard ->
                lastDashboard = dashboard
                val defaultIndex = dashboard.weeks.lastIndex.takeIf { idx -> idx >= 0 }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weeks = dashboard.weeks,
                        routes = dashboard.routes,
                        daily = dashboard.daily,
                        summary = dashboard.summary,
                        selectedWeekIndex = defaultIndex,
                        selectedWeekLoads = emptyList(),
                    )
                }
                defaultIndex?.let { selectWeek(it) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: app.getString(R.string.analytics_error_loading))
                }
            }
        }
    }

    fun exportAnalytics() {
        val dashboard = lastDashboard ?: return
        viewModelScope.launch {
            AnalyticsExporter.exportToCsv(app, dashboard, _uiState.value.period)
                .onSuccess { file ->
                    _uiState.update { it.copy(exportPath = file.absolutePath, error = null) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            exportPath = null,
                            error = app.getString(R.string.export_csv_error),
                        )
                    }
                }
        }
    }

    fun clearExportPath() {
        _uiState.update { it.copy(exportPath = null) }
    }

}
