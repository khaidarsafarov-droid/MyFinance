package com.truckerload.presentation.screens.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.StatsSelectionSnapshot
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.RouteSortBy
import com.truckerload.domain.model.RouteStats
import com.truckerload.domain.model.StateRevenue
import com.truckerload.utils.extractStateFromLocation
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getMonthRange
import com.truckerload.utils.getPreviousMonth
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.shiftWeekNumberAndYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.util.Locale

data class StatsUiState(
    val weekNumber: Int = 0,
    val year: Int = 0,
    val calendarMonth: Int = 0,
    val calendarYear: Int = 0,
    val statsPeriod: StatsPeriod = StatsPeriod.WEEK,
    val weeksInMonth: List<com.truckerload.domain.model.WeekSummary> = emptyList(),
    val totalPaycheck: Double = 0.0,
    val totalMiles: Double = 0.0,
    val loadCount: Int = 0,
    val totalDiesel: Double = 0.0,
    val totalGross: Double = 0.0,
    val netProfit: Double = 0.0,
    val weekLabel: String = "",
    val periodLabel: String = "",
    val prevPaycheck: Double? = null,
    val prevMiles: Double? = null,
    val prevLoadCount: Int? = null,
    val prevDiesel: Double? = null,
    val prevGross: Double? = null,
    val prevNetProfit: Double? = null,
    val avgRpm: Double = 0.0,
    val prevAvgRpm: Double? = null,
    val routeStats: List<RouteStats> = emptyList(),
    val routeSortBy: RouteSortBy = RouteSortBy.RATE_PER_MILE,
    val topStatesByRevenue: List<StateRevenue> = emptyList(),
    val selectedStateCode: String = "KY",
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val weekRepository: WeekRepository,
    private val loadRepository: LoadRepository,
    private val selectedStateStore: SelectedStateStore,
    private val statsSelectionStore: StatsSelectionStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    private val minYear = currentYear - 5
    private val maxYear = currentYear + 5
    private val defaultMonth = java.util.Calendar.MARCH + 1

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState = _uiState.asStateFlow().stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())
    private val periodCache = mutableMapOf<StatsPeriod, CachedPeriodSnapshot>()

    private data class CachedPeriodSnapshot(
        val totalPaycheck: Double,
        val totalMiles: Double,
        val loadCount: Int,
        val totalDiesel: Double,
        val totalGross: Double,
        val netProfit: Double,
        val periodLabel: String,
        val prevPaycheck: Double?,
        val prevMiles: Double?,
        val prevLoadCount: Int?,
        val prevDiesel: Double?,
        val prevGross: Double?,
        val prevNetProfit: Double?,
        val avgRpm: Double,
        val prevAvgRpm: Double?,
        val routeStats: List<RouteStats>,
        val topStatesByRevenue: List<StateRevenue>
    )

    init {
        val cal = java.util.Calendar.getInstance()
        val (currentWeek, _) = getCurrentWeekNumberAndYear()
        val yr = cal.get(java.util.Calendar.YEAR).coerceIn(minYear, maxYear)
        val persisted = statsSelectionStore.read(
            defaultWeek = currentWeek.coerceIn(1, 53),
            defaultYear = yr,
            defaultMonth = defaultMonth
        )
        val restoredWeekYear = persisted.weekYear.coerceIn(minYear, maxYear)
        val restoredCalendarYear = persisted.calendarYear.coerceIn(minYear, maxYear)
        val periodFromHandle = savedStateHandle.get<String>(KEY_PERIOD)
            ?.let { runCatching { StatsPeriod.valueOf(it) }.getOrNull() }
        _uiState.update {
            it.copy(
                weekNumber = persisted.weekNumber.coerceIn(1, 53),
                year = restoredWeekYear,
                calendarMonth = persisted.calendarMonth.coerceIn(1, 12),
                calendarYear = restoredCalendarYear,
                statsPeriod = periodFromHandle ?: persisted.period,
                selectedStateCode = selectedStateStore.current()
            )
        }
        viewModelScope.launch {
            val state = _uiState.value
            val summaries = weekRepository.getWeeksInMonthSummaries(state.calendarMonth, state.calendarYear)
            val selected = summaries.firstOrNull {
                it.weekNumber == state.weekNumber && it.year == state.year
            } ?: summaries.firstOrNull()
            val selectedWeek = selected?.weekNumber ?: state.weekNumber.coerceIn(1, 53)
            val selectedYear = selected?.year ?: state.year.coerceIn(minYear, maxYear)
            _uiState.update {
                it.copy(
                    weeksInMonth = summaries,
                    weekNumber = selectedWeek,
                    year = selectedYear
                )
            }
            persistSelection()
            loadStatsForCurrentPeriod()
        }
    }

    fun setStatsPeriod(period: StatsPeriod) {
        savedStateHandle[KEY_PERIOD] = period.name
        val cached = periodCache[period]
        _uiState.update {
            if (cached != null) {
                it.copy(
                    statsPeriod = period,
                    totalPaycheck = cached.totalPaycheck,
                    totalMiles = cached.totalMiles,
                    loadCount = cached.loadCount,
                    totalDiesel = cached.totalDiesel,
                    totalGross = cached.totalGross,
                    netProfit = cached.netProfit,
                    periodLabel = cached.periodLabel,
                    prevPaycheck = cached.prevPaycheck,
                    prevMiles = cached.prevMiles,
                    prevLoadCount = cached.prevLoadCount,
                    prevDiesel = cached.prevDiesel,
                    prevGross = cached.prevGross,
                    prevNetProfit = cached.prevNetProfit,
                    avgRpm = cached.avgRpm,
                    prevAvgRpm = cached.prevAvgRpm,
                    routeStats = cached.routeStats,
                    topStatesByRevenue = cached.topStatesByRevenue,
                    isLoading = false
                )
            } else {
                it.copy(statsPeriod = period, isLoading = true)
            }
        }
        persistSelection()
        loadStatsForCurrentPeriod()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadStatsForCurrentPeriod()
        loadWeeksInMonth(_uiState.value.calendarMonth, _uiState.value.calendarYear)
    }

    fun setRouteSortBy(sortBy: RouteSortBy) {
        val routes = _uiState.value.routeStats
        val sorted = when (sortBy) {
            RouteSortBy.RATE_PER_MILE -> routes.sortedByDescending { it.ratePerMile }
            RouteSortBy.TOTAL_EARNED -> routes.sortedByDescending { it.totalEarned }
            RouteSortBy.LOAD_COUNT -> routes.sortedByDescending { it.totalLoads }
        }
        _uiState.update { it.copy(routeSortBy = sortBy, routeStats = sorted) }
    }

    fun setSelectedState(code: String) {
        selectedStateStore.save(code)
        _uiState.update { it.copy(selectedStateCode = code) }
    }

    fun resetFiltersToDefault() {
        val cal = java.util.Calendar.getInstance()
        val (currentWeek, _) = getCurrentWeekNumberAndYear()
        val yr = cal.get(java.util.Calendar.YEAR).coerceIn(minYear, maxYear)
        _uiState.update {
            it.copy(
                statsPeriod = StatsPeriod.WEEK,
                weekNumber = currentWeek.coerceIn(1, 53),
                year = yr,
                calendarMonth = defaultMonth,
                calendarYear = yr,
                selectedStateCode = "KY",
                isLoading = true
            )
        }
        selectedStateStore.save("KY")
        persistSelection()
        periodCache.clear()
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(defaultMonth, yr)
            val selected = summaries.firstOrNull { it.weekNumber == currentWeek && it.year == yr } ?: summaries.firstOrNull()
            _uiState.update {
                it.copy(
                    weeksInMonth = summaries,
                    weekNumber = selected?.weekNumber ?: currentWeek.coerceIn(1, 53),
                    year = selected?.year ?: yr
                )
            }
            persistSelection()
            loadStatsForCurrentPeriod()
        }
    }

    fun setMonthYear(month: Int, year: Int) {
        val m = month.coerceIn(1, 12)
        val yr = year.coerceIn(minYear, maxYear)
        _uiState.update { it.copy(calendarMonth = m, calendarYear = yr, isLoading = true) }
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(m, yr)
            val state = _uiState.value
            val selectedInList = summaries.any { it.weekNumber == state.weekNumber && it.year == state.year }
            val selected = if (selectedInList) {
                state.weekNumber to state.year
            } else {
                summaries.firstOrNull()?.let { it.weekNumber to it.year } ?: (state.weekNumber to state.year)
            }
            _uiState.update {
                it.copy(
                    calendarMonth = m,
                    calendarYear = yr,
                    weeksInMonth = summaries,
                    weekNumber = selected.first,
                    year = selected.second
                )
            }
            persistSelection()
            loadStatsForCurrentPeriod()
        }
    }

    fun selectWeek(weekNumber: Int, weekYear: Int) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, weekYear)
        cal.set(java.util.Calendar.WEEK_OF_YEAR, weekNumber)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)
        _uiState.update {
            it.copy(
                weekNumber = weekNumber,
                year = weekYear,
                calendarMonth = month,
                calendarYear = year
            )
        }
        persistSelection()
        loadWeeksInMonth(month, year)
        loadStatsForCurrentPeriod()
    }

    fun previousWeek() {
        val (w, y) = shiftWeekNumberAndYear(_uiState.value.weekNumber, _uiState.value.year, -1)
        selectWeek(w, y)
    }

    fun nextWeek() {
        val (w, y) = shiftWeekNumberAndYear(_uiState.value.weekNumber, _uiState.value.year, 1)
        selectWeek(w, y)
    }

    private fun loadStatsForCurrentPeriod() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val s = _uiState.value
                when (s.statsPeriod) {
                    StatsPeriod.WEEK -> loadWeekPeriod(s.weekNumber, s.year)
                    StatsPeriod.MONTH -> loadMonthPeriod(s.calendarMonth, s.calendarYear)
                    StatsPeriod.YEAR -> loadYearPeriod(s.calendarYear)
                }
            }
        }
    }

    private suspend fun loadWeekPeriod(weekNumber: Int, year: Int) {
        val (startDate, endDate, weekLabel) = getWeekRange(weekNumber, year)
        val loadList = loadRepository.getLoadsByWeek(weekNumber, year).first()
        val summary = weekRepository.getWeekSummaryOnce(weekNumber, year)
        val prevSummary = getPreviousWeekSummary(weekNumber, year)
        val prevLoads = prevSummary?.let { loadRepository.getLoadsByWeek(it.weekNumber, it.year).first() } ?: emptyList()
        applyPeriodResult(
            loads = loadList,
            totalPaycheck = summary.paycheckAmount,
            totalMiles = summary.totalMiles,
            loadCount = summary.loadsCount,
            totalDiesel = summary.dieselAmount,
            totalGross = summary.totalLoadRate,
            netProfit = summary.netProfit,
            periodLabel = weekLabel,
            prevPaycheck = prevSummary?.paycheckAmount,
            prevMiles = prevSummary?.totalMiles,
            prevLoadCount = prevSummary?.loadsCount,
            prevDiesel = prevSummary?.dieselAmount,
            prevGross = prevSummary?.totalLoadRate,
            prevNetProfit = prevSummary?.netProfit,
            prevLoads = prevLoads
        )
    }

    private suspend fun loadMonthPeriod(month: Int, year: Int) {
        val (startDate, endDate) = getMonthRange(month, year)
        val periodLabel = "${getMonthName(month)} $year"
        val summary = weekRepository.getPeriodSummaryOnce(startDate, endDate, periodLabel)
        val (prevM, prevY) = getPreviousMonth(month, year)
        val (prevStart, prevEnd) = getMonthRange(prevM, prevY)
        val prevSummary = weekRepository.getPeriodSummaryOnce(prevStart, prevEnd, "")
        val prevLoads = loadRepository.getLoadsByDateRangeOnce(prevStart, prevEnd)
        applyPeriodResult(
            loads = loadRepository.getLoadsByDateRangeOnce(startDate, endDate),
            totalPaycheck = summary.paycheckAmount,
            totalMiles = summary.totalMiles,
            loadCount = summary.loadsCount,
            totalDiesel = summary.dieselAmount,
            totalGross = summary.totalLoadRate,
            netProfit = summary.netProfit,
            periodLabel = periodLabel,
            prevPaycheck = prevSummary.paycheckAmount,
            prevMiles = prevSummary.totalMiles,
            prevLoadCount = prevSummary.loadsCount,
            prevDiesel = prevSummary.dieselAmount,
            prevGross = prevSummary.totalLoadRate,
            prevNetProfit = prevSummary.netProfit,
            prevLoads = prevLoads
        )
    }

    private suspend fun loadYearPeriod(year: Int) {
        val startDate = "%04d-01-01".format(year)
        val endDate = "%04d-12-31".format(year)
        val periodLabel = "$year"
        val summary = weekRepository.getPeriodSummaryOnce(startDate, endDate, periodLabel)
        val prevY = year - 1
        val prevStart = "%04d-01-01".format(prevY)
        val prevEnd = "%04d-12-31".format(prevY)
        val prevSummary = weekRepository.getPeriodSummaryOnce(prevStart, prevEnd, "")
        val prevLoads = loadRepository.getLoadsByDateRangeOnce(prevStart, prevEnd)
        applyPeriodResult(
            loads = loadRepository.getLoadsByDateRangeOnce(startDate, endDate),
            totalPaycheck = summary.paycheckAmount,
            totalMiles = summary.totalMiles,
            loadCount = summary.loadsCount,
            totalDiesel = summary.dieselAmount,
            totalGross = summary.totalLoadRate,
            netProfit = summary.netProfit,
            periodLabel = periodLabel,
            prevPaycheck = prevSummary.paycheckAmount,
            prevMiles = prevSummary.totalMiles,
            prevLoadCount = prevSummary.loadsCount,
            prevDiesel = prevSummary.dieselAmount,
            prevGross = prevSummary.totalLoadRate,
            prevNetProfit = prevSummary.netProfit,
            prevLoads = prevLoads
        )
    }

    private fun getMonthName(month: Int): String {
        val short = DateFormatSymbols(Locale.getDefault())
            .shortMonths
            .getOrNull((month - 1).coerceIn(0, 11))
            .orEmpty()
        return short.replace(".", "").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private suspend fun applyPeriodResult(
        loads: List<Load>,
        totalPaycheck: Double,
        totalMiles: Double,
        loadCount: Int,
        totalDiesel: Double,
        totalGross: Double,
        netProfit: Double,
        periodLabel: String,
        prevPaycheck: Double?,
        prevMiles: Double?,
        prevLoadCount: Int?,
        prevDiesel: Double?,
        prevGross: Double?,
        prevNetProfit: Double?,
        prevLoads: List<Load>
    ) {
        val routes = computeRouteStats(loads)
        val sortBy = _uiState.value.routeSortBy
        val sortedRoutes = when (sortBy) {
            RouteSortBy.RATE_PER_MILE -> routes.sortedByDescending { it.ratePerMile }
            RouteSortBy.TOTAL_EARNED -> routes.sortedByDescending { it.totalEarned }
            RouteSortBy.LOAD_COUNT -> routes.sortedByDescending { it.totalLoads }
        }
        val topStates = computeTopStatesByRevenue(loads)
        val avgRpm = if (totalMiles > 0) totalGross / totalMiles else 0.0
        val prevTotalMiles = prevLoads.sumOf { it.totalMiles }
        val prevTotalGross = prevLoads.sumOf { it.totalRate }
        val prevAvgRpm = if (prevTotalMiles > 0) prevTotalGross / prevTotalMiles else null

        val weekLabel = if (_uiState.value.statsPeriod == StatsPeriod.WEEK) periodLabel else ""

        _uiState.update {
            it.copy(
                totalPaycheck = totalPaycheck,
                totalMiles = totalMiles,
                loadCount = loadCount,
                totalDiesel = totalDiesel,
                totalGross = totalGross,
                netProfit = netProfit,
                weekLabel = weekLabel,
                periodLabel = periodLabel,
                prevPaycheck = prevPaycheck,
                prevMiles = prevMiles,
                prevLoadCount = prevLoadCount,
                prevDiesel = prevDiesel,
                prevGross = prevGross,
                prevNetProfit = prevNetProfit,
                avgRpm = avgRpm,
                prevAvgRpm = prevAvgRpm,
                routeStats = sortedRoutes,
                topStatesByRevenue = topStates,
                isLoading = false
            )
        }
        periodCache[_uiState.value.statsPeriod] = CachedPeriodSnapshot(
            totalPaycheck = totalPaycheck,
            totalMiles = totalMiles,
            loadCount = loadCount,
            totalDiesel = totalDiesel,
            totalGross = totalGross,
            netProfit = netProfit,
            periodLabel = periodLabel,
            prevPaycheck = prevPaycheck,
            prevMiles = prevMiles,
            prevLoadCount = prevLoadCount,
            prevDiesel = prevDiesel,
            prevGross = prevGross,
            prevNetProfit = prevNetProfit,
            avgRpm = avgRpm,
            prevAvgRpm = prevAvgRpm,
            routeStats = sortedRoutes,
            topStatesByRevenue = topStates
        )
    }

    private fun computeTopStatesByRevenue(loads: List<Load>): List<StateRevenue> {
        val totalRevenue = loads.sumOf { it.totalRate }
        if (totalRevenue <= 0) return emptyList()
        val byState = loads
            .filter { it.pointB.isNotBlank() }
            .mapNotNull { load ->
                extractStateFromLocation(load.pointB)?.let { state -> Triple(state, load.totalRate, load) }
            }
            .groupBy { it.first }
            .mapValues { (_, list) ->
                list.sumOf { it.second } to list.size
            }
        return byState
            .toList()
            .sortedByDescending { it.second.first }
            .take(5)
            .map { (state, pair) ->
                StateRevenue(
                    state = state,
                    revenue = pair.first,
                    trips = pair.second,
                    shareOfTotal = (pair.first / totalRevenue).toFloat()
                )
            }
    }

    private fun computeRouteStats(loads: List<Load>): List<RouteStats> {
        val grouped = loads
            .filter { it.pointA.isNotBlank() && it.pointB.isNotBlank() }
            .groupBy { "${it.pointA} → ${it.pointB}" }
        return grouped
            .filter { it.value.size >= 2 }
            .map { (key, list) ->
                val first = list.first()
                val totalEarned = list.sumOf { it.totalRate }
                val totalMiles = list.sumOf { it.totalMiles }
                val rates = list.map { it.totalRate }
                RouteStats(
                    pointA = first.pointA,
                    pointB = first.pointB,
                    routeKey = key,
                    totalLoads = list.size,
                    totalEarned = totalEarned,
                    totalMiles = totalMiles,
                    avgRate = totalEarned / list.size,
                    avgMiles = totalMiles / list.size,
                    ratePerMile = if (totalMiles > 0) totalEarned / totalMiles else 0.0,
                    bestLoad = rates.maxOrNull() ?: 0.0,
                    worstLoad = rates.minOrNull() ?: 0.0,
                    lastUsed = list.maxByOrNull { it.date }?.date ?: ""
                )
            }
    }

    private suspend fun getPreviousWeekSummary(weekNumber: Int, year: Int): com.truckerload.domain.model.WeekSummary? {
        val (prevWeek, prevYear) = shiftWeekNumberAndYear(weekNumber, year, -1)
        return runCatching {
            weekRepository.getWeekSummaryOnce(prevWeek, prevYear)
        }.onFailure { e ->
            android.util.Log.w("StatsViewModel", "Previous week summary failed", e)
        }.getOrNull()
    }

    private fun loadWeeksInMonth(month: Int, year: Int) {
        viewModelScope.launch {
            val summaries = weekRepository.getWeeksInMonthSummaries(month, year)
            _uiState.update { it.copy(weeksInMonth = summaries) }
        }
    }

    private fun persistSelection() {
        val s = _uiState.value
        statsSelectionStore.save(
            StatsSelectionSnapshot(
                period = s.statsPeriod,
                weekNumber = s.weekNumber,
                weekYear = s.year,
                calendarMonth = s.calendarMonth,
                calendarYear = s.calendarYear
            )
        )
    }

    class Factory(
        private val weekRepository: WeekRepository,
        private val loadRepository: LoadRepository,
        private val selectedStateStore: SelectedStateStore,
        private val statsSelectionStore: StatsSelectionStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            StatsViewModel(
                weekRepository,
                loadRepository,
                selectedStateStore,
                statsSelectionStore,
                extras.createSavedStateHandle(),
            ) as T
    }

    companion object {
        private const val KEY_PERIOD = "stats_period"
    }
}
