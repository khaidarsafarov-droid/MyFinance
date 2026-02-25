package com.truckerload.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.utils.getCurrentWeekNumberAndYear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val weekNumber: Int = 0,
    val year: Int = 0,
    val calendarMonth: Int = 0,
    val calendarYear: Int = 0,
    val weeksInMonth: List<com.truckerload.domain.model.WeekSummary> = emptyList(),
    val totalPaycheck: Double = 0.0,
    val totalMiles: Double = 0.0,
    val loadCount: Int = 0,
    val totalDiesel: Double = 0.0,
    val totalGross: Double = 0.0,
    val netProfit: Double = 0.0,
    val weekLabel: String = ""
)

class StatsViewModel(
    private val weekRepository: WeekRepository,
    private val loadRepository: LoadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState = _uiState.asStateFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    init {
        val cal = java.util.Calendar.getInstance()
        val (w, y) = getCurrentWeekNumberAndYear()
        val m = cal.get(java.util.Calendar.MONTH) + 1
        _uiState.update { it.copy(weekNumber = w, year = y, calendarMonth = m, calendarYear = cal.get(java.util.Calendar.YEAR)) }
        loadWeekStats(w, y)
        loadWeeksInMonth(m, cal.get(java.util.Calendar.YEAR))
    }

    fun setMonthYear(month: Int, year: Int) {
        _uiState.update { it.copy(calendarMonth = month, calendarYear = year) }
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(month, year)
            _uiState.update { it.copy(weeksInMonth = summaries) }
        }
    }

    fun selectWeek(weekNumber: Int, weekYear: Int) {
        _uiState.update { it.copy(weekNumber = weekNumber, year = weekYear) }
        loadWeekStats(weekNumber, weekYear)
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, weekYear)
        cal.set(java.util.Calendar.WEEK_OF_YEAR, weekNumber)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)
        _uiState.update { it.copy(calendarMonth = month, calendarYear = year) }
        loadWeeksInMonth(month, year)
    }

    private fun loadWeekStats(weekNumber: Int, year: Int) {
        viewModelScope.launch {
            val summary = weekRepository.getWeekSummaryOnce(weekNumber, year)
            _uiState.update {
                it.copy(
                    totalPaycheck = summary.paycheckAmount,
                    totalMiles = summary.totalMiles,
                    loadCount = summary.loadsCount,
                    totalDiesel = summary.dieselAmount,
                    totalGross = summary.totalLoadRate,
                    netProfit = summary.netProfit,
                    weekLabel = summary.weekLabel
                )
            }
        }
    }

    private fun loadWeeksInMonth(month: Int, year: Int) {
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(month, year)
            _uiState.update { it.copy(weeksInMonth = summaries) }
        }
    }

    class Factory(
        private val weekRepository: WeekRepository,
        private val loadRepository: LoadRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(weekRepository, loadRepository) as T
    }
}
