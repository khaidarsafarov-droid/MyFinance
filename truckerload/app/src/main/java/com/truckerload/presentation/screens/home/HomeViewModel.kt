package com.truckerload.presentation.screens.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotHealth
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.domain.model.Load
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.LoadDateIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

data class HomeUiState(
    val loads: List<Load> = emptyList(),
    val searchQuery: String = "",
    val filter: LoadFilter = LoadFilter.THIS_WEEK,
    val selectedYear: Int? = null,
    val selectedWeekStart: String? = null,
    val selectedWeekEnd: String? = null,
    val selectedWeekLabel: String = "",
    val selectedDate: String? = null,
    val selectedDateLabel: String = "",
    val isSearchExpanded: Boolean = false,
    val botStatusActive: Boolean = false
)

enum class LoadFilter { ALL, YESTERDAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, CALENDAR_WEEK, CALENDAR_DATE, DISPUTE }

data class YearSection(
    val year: Int,
    val loadCount: Int,
    val totalRate: Double,
    val totalMiles: Double,
    val months: List<MonthSection>
)

data class MonthSection(
    val year: Int,
    val month: Int,
    val monthName: String,
    val loads: List<Load>
)

sealed class HomeListItem {
    data class YearHeader(val section: YearSection) : HomeListItem()
    data class MonthHeader(val section: MonthSection) : HomeListItem()
    data class FilteredSectionHeader(val label: String, val totals: LoadFilterUseCase.Totals) : HomeListItem()
    data class LoadItem(val load: Load) : HomeListItem()
}

/** Поля, влияющие на фильтрацию — отдельно от UI-флагов (поиск expanded и т.д.). */
private data class HomeFilterState(
    val filter: LoadFilter = LoadFilter.THIS_WEEK,
    val searchQuery: String = "",
    val selectedDate: String? = null,
    val selectedWeekStart: String? = null,
    val selectedWeekEnd: String? = null,
    val selectedYear: Int? = null,
    val selectedDateLabel: String = "",
    val selectedWeekLabel: String = "",
)

class HomeViewModel(
    private val loadRepository: LoadRepository,
    private val isBotConfigured: Boolean = false,
    private val app: Application
) : ViewModel() {

    private val filterUseCase = LoadFilterUseCase()

    /** Одна подписка на Room — вместо двух параллельных watchLoads(). */
    private val loadsFromDb: StateFlow<List<Load>> = loadRepository.watchLoads()
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private val _initialLoadDone = MutableStateFlow(false)

    /** true до первого эмита из Room. */
    val isInitialLoading: StateFlow<Boolean> = _initialLoadDone
        .map { done -> !done }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = true)

    private val _uiState = MutableStateFlow(HomeUiState(botStatusActive = isBotConfigured))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Только поля фильтра — не пересчитываем список при isSearchExpanded и прочих UI-флагах. */
    private val filterState: StateFlow<HomeFilterState> = _uiState
        .map { state ->
            HomeFilterState(
                filter = state.filter,
                searchQuery = state.searchQuery,
                selectedDate = state.selectedDate,
                selectedWeekStart = state.selectedWeekStart,
                selectedWeekEnd = state.selectedWeekEnd,
                selectedYear = state.selectedYear,
                selectedDateLabel = state.selectedDateLabel,
                selectedWeekLabel = state.selectedWeekLabel,
            )
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeFilterState(),
        )

    /** Оптимистичные обновления: loadId -> Load. При сбое сохранения — откат через revertOptimisticUpdate. */
    private val _optimisticOverlay = MutableStateFlow<Map<String, Load>>(emptyMap())

    /** Результат фильтрации: список, итоги, даты с грузами (для индикаторов календаря). */
    data class FilteredResult(
        val loads: List<Load>,
        val totals: LoadFilterUseCase.Totals,
        val datesWithLoads: Set<String>
    )

    /** Фильтрованный список + итоги + индекс дат. Индекс пересчитывается только при смене merged loads. */
    val filteredLoadsAndTotals: StateFlow<FilteredResult> = combine(
        loadsFromDb,
        _optimisticOverlay,
        filterState,
    ) { loads, overlay, filter ->
        val base = loads.map { overlay[it.id] ?: it }
        val loadIds = loads.map { it.id }.toSet()
        val newLoads = overlay.values.filter { it.id !in loadIds }
        val merged = base + newLoads
        val dateIndex = LoadDateIndex.build(merged)
        val filtered = filterUseCase.filterLoads(
            loads = merged,
            filter = filter.filter,
            searchQuery = filter.searchQuery,
            selectedDate = filter.selectedDate,
            selectedWeekStart = filter.selectedWeekStart,
            selectedWeekEnd = filter.selectedWeekEnd,
            selectedYear = filter.selectedYear,
            dateIndex = dateIndex
        )
        FilteredResult(
            loads = filtered,
            totals = filterUseCase.calculateTotals(filtered),
            datesWithLoads = dateIndex.keys.toSet()
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = FilteredResult(emptyList(), LoadFilterUseCase.Totals(0, 0.0, 0.0), emptySet()))

    init {
        viewModelScope.launch {
            loadsFromDb.collect { list ->
                if (!_initialLoadDone.value) {
                    _initialLoadDone.value = true
                }
                _uiState.update { it.copy(loads = list) }
                _optimisticOverlay.update { current ->
                    val ids = list.map { it.id }.toSet()
                    current.filterKeys { it !in ids }
                }
            }
        }
        // Виджет обновляем с debounce — не на каждый символ поиска / оптимистичный оверлей.
        viewModelScope.launch {
            loadsFromDb
                .debounce(400)
                .collect { WidgetDataUpdater.updateWidgetData(app) }
        }
        if (isBotConfigured) {
            viewModelScope.launch {
                val token = TelegramTokenStore(app).getToken()
                val health = withContext(Dispatchers.IO) { TelegramBotHealth.check(token) }
                _uiState.update { it.copy(botStatusActive = health.ok) }
                if (health.ok) {
                    TelegramBotForegroundService.start(app)
                }
            }
        }
    }

    /** Оптимистичное обновление: сразу отобразить изменения в UI до сохранения в БД. */
    fun applyOptimisticUpdate(load: Load) {
        _optimisticOverlay.update { it + (load.id to load) }
    }

    /** Откат оптимистичного обновления при ошибке сохранения. */
    fun revertOptimisticUpdate(loadId: String) {
        _optimisticOverlay.update { it - loadId }
    }

    fun deleteLoad(loadId: String) {
        viewModelScope.launch {
            loadRepository.deleteLoad(loadId)
            WidgetDataUpdater.updateWidgetData(app.applicationContext)
        }
    }

    fun setSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }
    fun setSearchExpanded(expanded: Boolean) { _uiState.update { it.copy(isSearchExpanded = expanded) } }

    fun setFilter(filter: LoadFilter) {
        _uiState.update {
            it.copy(
                filter = filter,
                selectedYear = if (filter != LoadFilter.ALL) null else it.selectedYear
            )
        }
    }

    fun setSelectedYear(year: Int?) {
        _uiState.update { it.copy(selectedYear = year, filter = LoadFilter.ALL) }
    }

    fun selectDate(date: String) {
        val label = formatDateLabel(date)
        val (wn, wy) = getWeekNumberAndYearFromDate(date)
        val (ws, we, wl) = getWeekRange(wn, wy)
        _uiState.update {
            it.copy(
                filter = LoadFilter.CALENDAR_WEEK,
                selectedDate = date,
                selectedDateLabel = label,
                selectedWeekStart = ws,
                selectedWeekEnd = we,
                selectedWeekLabel = wl
            )
        }
    }

    private fun formatDateLabel(date: String): String {
        if (date.length < 10) return date
        val parts = date.split("-")
        if (parts.size != 3) return date
        val (y, m, d) = parts
        val mi = m.toIntOrNull() ?: return date
        val short = DateFormatSymbols(Locale.getDefault())
            .shortMonths
            .getOrNull((mi - 1).coerceIn(0, 11))
            .orEmpty()
            .replace(".", "")
            .lowercase(Locale.getDefault())
        return "$d $short $y"
    }

    fun selectDateFromCalendar(dateMillis: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        selectDate("%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)))
    }

    /** Список годов с грузами для селектора. */
    fun availableYears(): List<Int> = _uiState.value.loads
        .mapNotNull { load -> if (load.date.length >= 4) load.date.substring(0, 4).toIntOrNull() else null }
        .distinct()
        .sortedDescending()
        .ifEmpty { listOf(Calendar.getInstance().get(Calendar.YEAR)) }

    /** Заголовок с итогами выбранного периода — показывается над фильтром на главном экране. */
    fun periodSummaryHeader(totals: LoadFilterUseCase.Totals): HomeListItem.FilteredSectionHeader? {
        val state = _uiState.value
        val label = when (state.filter) {
            LoadFilter.CALENDAR_DATE -> if (state.selectedDateLabel.isNotBlank()) {
                formatFilterLabel(state.selectedDateLabel, totals.loadCount)
            } else {
                null
            }
            LoadFilter.CALENDAR_WEEK -> if (state.selectedWeekLabel.isNotBlank()) {
                formatFilterLabel(state.selectedWeekLabel, totals.loadCount)
            } else {
                null
            }
            LoadFilter.YESTERDAY -> formatFilterLabel(app.getString(R.string.home_filter_yesterday), totals.loadCount)
            LoadFilter.THIS_WEEK -> formatFilterLabel(app.getString(R.string.home_filter_this_week), totals.loadCount)
            LoadFilter.LAST_WEEK -> formatFilterLabel(app.getString(R.string.home_filter_last_week), totals.loadCount)
            LoadFilter.THIS_MONTH -> formatFilterLabel(app.getString(R.string.home_filter_this_month), totals.loadCount)
            LoadFilter.DISPUTE -> formatFilterLabel(app.getString(R.string.home_filter_dispute), totals.loadCount)
            LoadFilter.ALL -> if (state.selectedYear != null) {
                app.getString(
                    R.string.home_year_selected_header,
                    state.selectedYear ?: Calendar.getInstance().get(Calendar.YEAR),
                    totals.loadCount,
                    loadWord(totals.loadCount),
                )
            } else {
                null
            }
        }
        return label?.let { HomeListItem.FilteredSectionHeader(it, totals) }
    }

    /** Плоский список для LazyColumn. */
    fun flattenedListItems(
        filteredLoads: List<Load>,
        totals: LoadFilterUseCase.Totals
    ): List<HomeListItem> {
        val state = _uiState.value

        if (state.filter != LoadFilter.ALL) {
            return filteredLoads.map { HomeListItem.LoadItem(it) }
        }

        if (state.selectedYear != null) {
            val yearLoads = filteredLoads
            val sections = buildYearMonthSections(yearLoads)
            val result = mutableListOf<HomeListItem>()
            for (ys in sections) {
                result.add(HomeListItem.YearHeader(ys))
                for (ms in ys.months) {
                    result.add(HomeListItem.MonthHeader(ms))
                    result.addAll(ms.loads.map { HomeListItem.LoadItem(it) })
                }
            }
            return result
        }

        val sections = groupedLoadsByYearMonth(filteredLoads)
        val result = mutableListOf<HomeListItem>()
        for (yearSection in sections) {
            result.add(HomeListItem.YearHeader(yearSection))
            for (monthSection in yearSection.months) {
                result.add(HomeListItem.MonthHeader(monthSection))
                result.addAll(monthSection.loads.map { HomeListItem.LoadItem(it) })
            }
        }
        return result
    }

    private fun loadWord(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> app.getString(R.string.home_load_word_one)
        n % 10 in 2..4 && (n % 100 < 10 || n % 100 >= 20) -> app.getString(R.string.home_load_word_few)
        else -> app.getString(R.string.home_load_word_many)
    }

    private fun formatFilterLabel(baseLabel: String, count: Int): String =
        app.getString(R.string.home_filter_label_with_count, baseLabel, count, loadWord(count))

    private fun monthName(month: Int): String {
        val long = DateFormatSymbols(Locale.getDefault())
            .months
            .getOrNull((month - 1).coerceIn(0, 11))
            .orEmpty()
        return long.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun groupedLoadsByYearMonth(loads: List<Load>): List<YearSection> {
        if (loads.isEmpty()) return emptyList()
        val byYear = loads.groupBy { load ->
            if (load.date.length >= 4) load.date.substring(0, 4).toIntOrNull() ?: 0 else 0
        }.filterKeys { it > 0 }

        return byYear.keys.sortedDescending().map { year ->
            val yearLoads = byYear[year] ?: emptyList()
            val byMonth = yearLoads.groupBy { load ->
                if (load.date.length >= 7) load.date.substring(5, 7).toIntOrNull() ?: 0 else 0
            }.filterKeys { it in 1..12 }

            val monthSections = byMonth.keys.sortedDescending().map { month ->
                val monthLoads = (byMonth[month] ?: emptyList())
                    .sortedWith(compareByDescending<Load> { it.date }.thenByDescending { it.parsedAt })
                MonthSection(year, month, monthName(month), monthLoads)
            }
            YearSection(year, yearLoads.size, yearLoads.sumOf { it.totalRate }, yearLoads.sumOf { it.totalMiles }, monthSections)
        }
    }

    private fun buildYearMonthSections(loads: List<Load>): List<YearSection> {
        if (loads.isEmpty()) return emptyList()
        return groupedLoadsByYearMonth(loads)
    }

    class Factory(
        private val loadRepository: LoadRepository,
        private val isBotConfigured: Boolean = false,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(loadRepository, isBotConfigured, context.applicationContext as Application) as T
    }
}
