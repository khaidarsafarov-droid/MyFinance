package com.truckerload.presentation.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.domain.usecase.ForecastService
import com.truckerload.domain.usecase.FuelAnalytics
import com.truckerload.domain.usecase.FuelAnalyticsService
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
    val weekNumber: Int = 1,
    val year: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val calendarMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
    val calendarYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val weekSummary: WeekSummary? = null,
    val weekLoads: List<com.truckerload.domain.model.Load> = emptyList(),
    val paycheck: com.truckerload.domain.model.Paycheck? = null,
    val dieselList: List<com.truckerload.domain.model.Diesel> = emptyList(),
    val weeksInMonth: List<WeekSummary> = emptyList(),
    val weekForecast: com.truckerload.domain.usecase.WeekForecast? = null,
    val fuelAnalytics: FuelAnalytics? = null
)

class FinanceViewModel(
    private val weekRepository: WeekRepository,
    private val loadRepository: LoadRepository,
    private val paycheckRepository: com.truckerload.data.repository.PaycheckRepository,
    private val dieselRepository: com.truckerload.data.repository.DieselRepository
) : ViewModel() {
    private val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    private val minYear = currentYear - 5
    private val maxYear = currentYear + 5
    private val defaultMonth = java.util.Calendar.MARCH + 1

    private val forecastService = ForecastService(weekRepository, loadRepository)
    private val fuelAnalyticsService = FuelAnalyticsService(dieselRepository, loadRepository)

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState = _uiState.asStateFlow().stateIn(viewModelScope, SharingStarted.Eagerly, FinanceUiState())

    init {
        val cal = java.util.Calendar.getInstance()
        val (currentWeek, _) = getCurrentWeekNumberAndYear()
        val yr = cal.get(java.util.Calendar.YEAR).coerceIn(minYear, maxYear)
        _uiState.update {
            it.copy(
                weekNumber = currentWeek.coerceIn(1, 53),
                year = yr,
                calendarMonth = defaultMonth,
                calendarYear = yr
            )
        }
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(defaultMonth, yr)
            val selected = summaries.firstOrNull { it.weekNumber == currentWeek && it.year == yr } ?: summaries.firstOrNull()
            val selectedWeek = selected?.weekNumber ?: currentWeek.coerceIn(1, 53)
            val selectedYear = selected?.year ?: yr
            _uiState.update {
                it.copy(
                    calendarMonth = defaultMonth,
                    calendarYear = yr,
                    weeksInMonth = summaries,
                    weekNumber = selectedWeek,
                    year = selectedYear
                )
            }
            setWeek(selectedWeek, selectedYear)
        }
    }

    /** Перезагружает данные. Основной поток — реактивный (Flow), обновление автоматическое. */
    fun refresh() {
        loadWeeksInMonth(_uiState.value.calendarMonth, _uiState.value.calendarYear)
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
            val forecast = forecastService.calculateForecast(weekNumber, year)
            _uiState.update { it.copy(weekForecast = forecast) }
        }
        viewModelScope.launch {
            val fuel = try { fuelAnalyticsService.calculateForWeek(weekNumber, year) } catch (_: Exception) { null }
            _uiState.update { it.copy(fuelAnalytics = fuel) }
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
        selectWeek(w, y)
    }

    fun nextWeek() {
        var w = _uiState.value.weekNumber + 1
        var y = _uiState.value.year
        if (w > 52) { w = 1; y++ }
        selectWeek(w, y)
    }

    fun setMonthYear(month: Int, year: Int) {
        val m = month.coerceIn(1, 12)
        val yr = year.coerceIn(minYear, maxYear)
        _uiState.update { it.copy(calendarMonth = m, calendarYear = yr) }
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(m, yr)
            val state = _uiState.value
            val selectedInList = summaries.any { it.weekNumber == state.weekNumber && it.year == state.year }
            val (selW, selY) = if (!selectedInList && summaries.isNotEmpty()) {
                summaries.first().let { it.weekNumber to it.year }
            } else {
                state.weekNumber to state.year
            }
            _uiState.update {
                it.copy(
                    weeksInMonth = summaries,
                    weekNumber = selW,
                    year = selY
                )
            }
            setWeek(selW, selY)
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
