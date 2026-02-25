package com.truckerload.presentation.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.domain.model.WeekSummary
import com.truckerload.utils.getCurrentWeekNumberAndYear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FinancePeriod { WEEK, MONTH, YEAR }

data class FinanceUiState(
    val period: FinancePeriod = FinancePeriod.WEEK,
    val weekNumber: Int = 0,
    val year: Int = 0,
    val calendarMonth: Int = 0,
    val calendarYear: Int = 0,
    val weekSummary: WeekSummary? = null,
    val weekLoads: List<com.truckerload.domain.model.Load> = emptyList(),
    val paycheck: com.truckerload.domain.model.Paycheck? = null,
    val dieselList: List<com.truckerload.domain.model.Diesel> = emptyList(),
    val weeksInMonth: List<WeekSummary> = emptyList()
)

class FinanceViewModel(
    private val weekRepository: WeekRepository,
    private val loadRepository: LoadRepository,
    private val paycheckRepository: com.truckerload.data.repository.PaycheckRepository,
    private val dieselRepository: com.truckerload.data.repository.DieselRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState = _uiState.asStateFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceUiState())

    init {
        val cal = java.util.Calendar.getInstance()
        val (w, y) = getCurrentWeekNumberAndYear()
        val m = cal.get(java.util.Calendar.MONTH) + 1
        _uiState.update { it.copy(weekNumber = w, year = y, calendarMonth = m, calendarYear = cal.get(java.util.Calendar.YEAR)) }
        setWeek(w, y)
        loadWeeksInMonth(m, cal.get(java.util.Calendar.YEAR))
    }

    fun setPeriod(period: FinancePeriod) {
        _uiState.update { it.copy(period = period) }
    }

    fun setWeek(weekNumber: Int, year: Int) {
        _uiState.update { it.copy(weekNumber = weekNumber, year = year) }
        viewModelScope.launch {
            weekRepository.getWeekSummary(weekNumber, year).collect { summary ->
                _uiState.update { it.copy(weekSummary = summary) }
            }
        }
        viewModelScope.launch {
            loadRepository.getLoadsByWeek(weekNumber, year).collect { loads ->
                _uiState.update { it.copy(weekLoads = loads) }
            }
        }
        viewModelScope.launch {
            val p = paycheckRepository.getPaycheckForWeek(weekNumber, year)
            _uiState.update { it.copy(paycheck = p) }
        }
        viewModelScope.launch {
            dieselRepository.getDieselForWeek(weekNumber, year).collect { list ->
                _uiState.update { it.copy(dieselList = list) }
            }
        }
    }

    fun previousWeek() {
        var w = _uiState.value.weekNumber - 1
        var y = _uiState.value.year
        if (w < 1) { w = 52; y-- }
        setWeek(w, y)
    }

    fun nextWeek() {
        var w = _uiState.value.weekNumber + 1
        var y = _uiState.value.year
        if (w > 52) { w = 1; y++ }
        setWeek(w, y)
    }

    fun setMonthYear(month: Int, year: Int) {
        _uiState.update { it.copy(calendarMonth = month, calendarYear = year) }
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(month, year)
            _uiState.update { it.copy(weeksInMonth = summaries) }
        }
    }

    private fun loadWeeksInMonth(month: Int, year: Int) {
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(month, year)
            _uiState.update { it.copy(weeksInMonth = summaries) }
        }
    }

    fun selectWeek(weekNumber: Int, weekYear: Int) {
        setWeek(weekNumber, weekYear)
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, weekYear)
        cal.set(java.util.Calendar.WEEK_OF_YEAR, weekNumber)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)
        _uiState.update { it.copy(calendarMonth = month, calendarYear = year) }
        loadWeeksInMonth(month, year)
    }

    class Factory(
        private val weekRepository: WeekRepository,
        private val loadRepository: LoadRepository,
        private val paycheckRepository: com.truckerload.data.repository.PaycheckRepository,
        private val dieselRepository: com.truckerload.data.repository.DieselRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FinanceViewModel(weekRepository, loadRepository, paycheckRepository, dieselRepository) as T
    }
}
