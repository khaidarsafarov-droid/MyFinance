package com.truckerload.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.utils.getCurrentWeekStartAndEnd
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getYesterdayDate
import com.truckerload.utils.getLastWeekStartAndEnd
import com.truckerload.utils.parseDateFromQuery
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.getWeekLabelShort
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loads: List<Load> = emptyList(),
    val searchQuery: String = "",
    val filter: LoadFilter = LoadFilter.ALL,
    val selectedWeekStart: String? = null,
    val selectedWeekEnd: String? = null,
    val selectedWeekLabel: String = "",
    val isSearchExpanded: Boolean = false,
    val botStatusActive: Boolean = false
)

enum class LoadFilter { ALL, YESTERDAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, CALENDAR_WEEK }

/** Секция года с итогами. Иерархия: Год > Месяц > День. */
data class YearSection(
    val year: Int,
    val loadCount: Int,
    val totalRate: Double,
    val totalMiles: Double,
    val months: List<MonthSection>
)

/** Секция месяца. */
data class MonthSection(
    val year: Int,
    val month: Int,
    val monthName: String,
    val loads: List<Load>
)

/** Элемент списка для LazyColumn (Year header / Month header / Load). */
sealed class HomeListItem {
    data class YearHeader(val section: YearSection) : HomeListItem()
    data class MonthHeader(val section: MonthSection) : HomeListItem()
    data class LoadItem(val load: Load) : HomeListItem()
}

private val MONTH_NAMES_RU = listOf(
    "", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

class HomeViewModel(
    private val loadRepository: LoadRepository,
    private val isBotConfigured: Boolean = false
) : ViewModel() {

    // Единственный источник правды — локальная БД (Room). Список лоудов всегда из телефона, не из Telegram.
    private val loadsFromDb: StateFlow<List<Load>> = loadRepository.getAllLoads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(HomeUiState(botStatusActive = isBotConfigured))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadsFromDb.collect { list ->
                _uiState.update { it.copy(loads = list) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSearchExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isSearchExpanded = expanded) }
    }

    fun setFilter(filter: LoadFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    /** Выбор даты по календарю — автоматически отображает неделю, содержащую эту дату. */
    fun selectDateFromCalendar(dateMillis: Long) {
        val (weekNumber, year) = getWeekNumberAndYearFromTimestamp(dateMillis)
        val (weekStart, weekEnd, _) = getWeekRange(weekNumber, year)
        val label = getWeekLabelShort(weekNumber, year)
        _uiState.update {
            it.copy(
                filter = LoadFilter.CALENDAR_WEEK,
                selectedWeekStart = weekStart,
                selectedWeekEnd = weekEnd,
                selectedWeekLabel = label
            )
        }
    }

    /** Filter loads by current tab and search. Uses load_date (load.date), creation_date (parsedAt) не меняется. */
    fun filteredLoads(): List<Load> {
        val state = _uiState.value
        var list = state.loads
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim()
            val parsedDate = parseDateFromQuery(q)
            list = if (parsedDate != null) {
                list.filter { it.date == parsedDate }
            } else {
                val qLower = q.lowercase()
                list.filter {
                    it.tripId.lowercase().contains(qLower) ||
                        it.pointA.lowercase().contains(qLower) ||
                        it.pointB.lowercase().contains(qLower) ||
                        it.date.contains(q)
                }
            }
        }
        list = when (state.filter) {
            LoadFilter.ALL -> list
            LoadFilter.YESTERDAY -> {
                val yesterday = getYesterdayDate()
                list.filter { it.date == yesterday }
            }
            LoadFilter.THIS_WEEK -> {
                val (weekStart, weekEnd) = getCurrentWeekStartAndEnd()
                list.filter { load ->
                    load.date.length >= 10 && load.date >= weekStart && load.date <= weekEnd
                }
            }
            LoadFilter.LAST_WEEK -> {
                val (start, end) = getLastWeekStartAndEnd()
                list.filter { load ->
                    load.date.length >= 10 && load.date >= start && load.date <= end
                }
            }
            LoadFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val prefix = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                list.filter { it.date.startsWith(prefix) }
            }
            LoadFilter.CALENDAR_WEEK -> {
                val start = state.selectedWeekStart
                val end = state.selectedWeekEnd
                if (start != null && end != null) {
                    list.filter { load ->
                        load.date.length >= 10 && load.date >= start && load.date <= end
                    }
                } else list
            }
        }
        return list.sortedWith(compareByDescending<Load> { it.date }.thenByDescending { it.parsedAt })
    }

    /** Группировка по году и месяцу. Только для filter == ALL. Год > Месяц > Грузы. */
    fun groupedLoadsByYearMonth(): List<YearSection> {
        val loads = filteredLoads()
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
                MonthSection(
                    year = year,
                    month = month,
                    monthName = MONTH_NAMES_RU.getOrElse(month) { "" },
                    loads = monthLoads
                )
            }

            YearSection(
                year = year,
                loadCount = yearLoads.size,
                totalRate = yearLoads.sumOf { it.totalRate },
                totalMiles = yearLoads.sumOf { it.totalMiles },
                months = monthSections
            )
        }
    }

    /** Плоский список для LazyColumn: YearHeader, MonthHeader, LoadItem. */
    fun flattenedListItems(): List<HomeListItem> {
        if (_uiState.value.filter != LoadFilter.ALL) {
            return filteredLoads().map { HomeListItem.LoadItem(it) }
        }
        if (_uiState.value.filter == LoadFilter.CALENDAR_WEEK && _uiState.value.selectedWeekLabel.isNotBlank()) {
            return filteredLoads().map { HomeListItem.LoadItem(it) }
        }
        val sections = groupedLoadsByYearMonth()
        val result = mutableListOf<HomeListItem>()
        for (yearSection in sections) {
            result.add(HomeListItem.YearHeader(yearSection))
            for (monthSection in yearSection.months) {
                result.add(HomeListItem.MonthHeader(monthSection))
                for (load in monthSection.loads) {
                    result.add(HomeListItem.LoadItem(load))
                }
            }
        }
        return result
    }

    class Factory(
        private val loadRepository: LoadRepository,
        private val isBotConfigured: Boolean = false
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(loadRepository, isBotConfigured) as T
    }
}
