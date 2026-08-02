package com.truckerload.presentation.screens.home

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotHealth
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.LoadDateIndex
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import com.truckerload.utils.FeedbackManager

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadRepository: LoadRepository,
    private val app: Application,
) : ViewModel() {

    companion object {
        private const val UNDO_DELETE_WINDOW_MS = 5_000L

        /** Foreground-сервис бота запускаем один раз за процесс, не при каждом recreate VM. */
        private val botServiceStarted = AtomicBoolean(false)
    }

    private val filterUseCase = LoadFilterUseCase()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Room subscription scoped by filter — week filters use reporting weekNumber/year. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val loadsFromDb: StateFlow<List<Load>> = _uiState
        .map { Triple(it.filter, it.selectedWeekStart, it.selectedWeekEnd) }
        .distinctUntilChanged()
        .flatMapLatest { (filter, weekStart, _) ->
            when (filter) {
                LoadFilter.THIS_WEEK -> {
                    val (w, y) = getCurrentWeekNumberAndYear()
                    loadRepository.getLoadsByWeek(w, y)
                }
                LoadFilter.LAST_WEEK -> {
                    val (w, y) = getPreviousWeekNumberAndYear()
                    loadRepository.getLoadsByWeek(w, y)
                }
                LoadFilter.CALENDAR_WEEK -> {
                    if (!weekStart.isNullOrBlank()) {
                        val (w, y) = getWeekNumberAndYearFromDate(weekStart)
                        loadRepository.getLoadsByWeek(w, y)
                    } else {
                        loadRepository.watchLoads()
                    }
                }
                else -> loadRepository.watchLoads()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Full journal for calendar dots — subscribed only while the calendar dialog is open
     * (see [calendarDatesWithLoads]), so Home does not load every load on cold start.
     */
    private val allLoadsForCalendar: Flow<List<Load>> = loadRepository.watchLoads()

    private val _initialLoadDone = MutableStateFlow(false)

    /** true до первого эмита из Room. */
    val isInitialLoading: StateFlow<Boolean> = _initialLoadDone
        .map { done -> !done }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    /** Immediate search text for the field; filtering uses [debouncedSearchQuery]. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val debouncedSearchQuery: StateFlow<String> = _searchQuery
        .debounce(250)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Только поля фильтра — не пересчитываем список при isSearchExpanded и прочих UI-флагах. */
    private val filterState: StateFlow<HomeFilterState> = combine(
        _uiState
            .map { state ->
                HomeFilterState(
                    filter = state.filter,
                    searchQuery = "",
                    selectedDate = state.selectedDate,
                    selectedWeekStart = state.selectedWeekStart,
                    selectedWeekEnd = state.selectedWeekEnd,
                    selectedYear = state.selectedYear,
                    selectedDateLabel = state.selectedDateLabel,
                    selectedWeekLabel = state.selectedWeekLabel,
                )
            }
            .distinctUntilChanged(),
        debouncedSearchQuery,
    ) { base, query -> base.copy(searchQuery = query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeFilterState(),
        )

    /** Оптимистичные обновления: loadId -> Load. При сбое сохранения — откат через revertOptimisticUpdate. */
    private val _optimisticOverlay = MutableStateFlow<Map<String, Load>>(emptyMap())

    /** IDs being deleted — excluded from merged list so they cannot reappear via overlay. */
    private val _pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())

    private val _pendingDeleteConfirmId = MutableStateFlow<String?>(null)
    val pendingDeleteConfirmId: StateFlow<String?> = _pendingDeleteConfirmId.asStateFlow()

    /** Load id waiting for Undo snackbar before hard delete. */
    private val _undoDeleteLoadId = MutableStateFlow<String?>(null)
    val undoDeleteLoadId: StateFlow<String?> = _undoDeleteLoadId.asStateFlow()

    private var undoDeleteJob: Job? = null

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    /** Bumped when delete is undone so swipe cards snap back. */
    private val _swipeSettleGeneration = MutableStateFlow(0)
    val swipeSettleGeneration: StateFlow<Int> = _swipeSettleGeneration.asStateFlow()

    /** Фильтрованный список + итоги. Calendar dots live in [calendarDatesWithLoads]. */
    val filteredLoadsAndTotals: StateFlow<FilteredResult> = combine(
        loadsFromDb,
        _optimisticOverlay,
        _pendingDeleteIds,
        filterState,
    ) { loads, overlay, pendingDeletes, filter ->
        val base = loads
            .filter { it.id !in pendingDeletes }
            .map { overlay[it.id] ?: it }
        val loadIds = loads.map { it.id }.toSet()
        val newLoads = overlay.values.filter { it.id !in loadIds && it.id !in pendingDeletes }
        val merged = base + newLoads
        val filtered = filterUseCase.filterLoads(
            loads = merged,
            filter = filter.filter,
            searchQuery = filter.searchQuery,
            selectedDate = filter.selectedDate,
            selectedWeekStart = filter.selectedWeekStart,
            selectedWeekEnd = filter.selectedWeekEnd,
            selectedYear = filter.selectedYear,
            dateIndex = null,
        )
        FilteredResult(
            loads = filtered,
            totals = filterUseCase.calculateTotals(filtered),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FilteredResult(emptyList(), LoadFilterUseCase.Totals(0, 0.0, 0.0)),
        )

    /**
     * Dates that have loads — collected only while the calendar dialog is composed so
     * opening Home does not hydrate the full journal into memory.
     */
    val calendarDatesWithLoads: StateFlow<Set<String>> = combine(
        allLoadsForCalendar,
        _optimisticOverlay,
        _pendingDeleteIds,
    ) { allLoads, overlay, pendingDeletes ->
        val calendarBase = allLoads
            .filter { it.id !in pendingDeletes }
            .map { overlay[it.id] ?: it }
        val calendarIds = allLoads.map { it.id }.toSet()
        val calendarMerged = calendarBase +
            overlay.values.filter { it.id !in calendarIds && it.id !in pendingDeletes }
        LoadDateIndex.build(calendarMerged).keys.toSet()
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )
    /**
     * True Room SQL paging for week / dispute journal filters.
     * Day/month filters stay in-memory so they use [getLoadDateRange] (active trip days),
     * matching header totals and calendar day selection.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val roomPagedLoads: Flow<PagingData<Load>> = filterState
        .map { Triple(it.filter, it.searchQuery, Triple(it.selectedDate, it.selectedWeekStart, it.selectedWeekEnd)) }
        .distinctUntilChanged()
        .flatMapLatest { (filter, searchQuery, dates) ->
            val (_, weekStart, _) = dates
            val trimmed = searchQuery.trim()
            when {
                trimmed.isNotEmpty() -> loadRepository.pagingLoads(searchQuery = trimmed)
                filter == LoadFilter.DISPUTE -> loadRepository.pagingLoads(activeDisputesOnly = true)
                filter == LoadFilter.THIS_WEEK -> {
                    val (w, y) = getCurrentWeekNumberAndYear()
                    loadRepository.pagingLoads(weekNumber = w, year = y)
                }
                filter == LoadFilter.LAST_WEEK -> {
                    val (w, y) = getPreviousWeekNumberAndYear()
                    loadRepository.pagingLoads(weekNumber = w, year = y)
                }
                filter == LoadFilter.CALENDAR_WEEK && !weekStart.isNullOrBlank() -> {
                    val (w, y) = getWeekNumberAndYearFromDate(weekStart)
                    loadRepository.pagingLoads(weekNumber = w, year = y)
                }
                else -> loadRepository.pagingLoads()
            }
        }
        .cachedIn(viewModelScope)

    /**
     * Room paging only for filters that match SQL on reporting week / dispute.
     * THIS_MONTH / YESTERDAY / CALENDAR_DATE need active-date-range logic in memory.
     */
    fun usesRoomPaging(filter: LoadFilter, selectedYear: Int?): Boolean {
        if (selectedYear != null) return false
        return when (filter) {
            LoadFilter.THIS_WEEK,
            LoadFilter.LAST_WEEK,
            LoadFilter.CALENDAR_WEEK,
            LoadFilter.DISPUTE,
            -> true
            else -> false
        }
    }
    init {
        viewModelScope.launch {
            loadsFromDb.collect { list ->
                if (!_initialLoadDone.value) {
                    _initialLoadDone.value = true
                }
                _uiState.update { it.copy(loads = list) }
                val ids = list.map { it.id }.toSet()
                // Keep only optimistic inserts not yet present in Room.
                _optimisticOverlay.update { current -> current.filterKeys { it !in ids } }
                // Once Room confirms deletion, clear pending delete markers.
                _pendingDeleteIds.update { pending -> pending.filter { it !in ids }.toSet() }
            }
        }
        // Виджет обновляем с debounce — не на каждый символ поиска / оптимистичный оверлей.
        viewModelScope.launch {
            loadsFromDb
                .debounce(400)
                .collect { runCatching { WidgetDataUpdater.updateWidgetData(app) } }
        }
        viewModelScope.launch {
            runCatching {
                val store = TelegramTokenStore(app)
                if (!store.hasToken()) return@runCatching
                val token = store.getToken()
                val health = withContext(Dispatchers.IO) { TelegramBotHealth.check(token) }
                _uiState.update { it.copy(botStatusActive = health.ok) }
                if (health.ok && botServiceStarted.compareAndSet(false, true)) {
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
        requestDeleteLoad(loadId)
    }

    /**
     * Soft-hides the load immediately and starts an Undo window before hard delete.
     * Keeps confirm APIs for callers that still use the dialog path.
     */
    fun requestDeleteLoad(loadId: String) {
        if (loadId.isBlank()) return
        FeedbackManager.onDeleteGesture()
        val previous = _undoDeleteLoadId.value
        if (previous != null && previous != loadId) {
            commitPendingDelete(previous)
        }
        undoDeleteJob?.cancel()
        _pendingDeleteConfirmId.value = null
        _optimisticOverlay.update { it - loadId }
        _pendingDeleteIds.update { it + loadId }
        _undoDeleteLoadId.value = loadId
        undoDeleteJob = viewModelScope.launch {
            delay(UNDO_DELETE_WINDOW_MS)
            commitPendingDelete(loadId)
        }
    }

    fun undoDeleteLoad() {
        val loadId = _undoDeleteLoadId.value ?: return
        undoDeleteJob?.cancel()
        undoDeleteJob = null
        _undoDeleteLoadId.value = null
        _pendingDeleteIds.update { it - loadId }
        _swipeSettleGeneration.update { it + 1 }
    }

    fun dismissDeleteLoad() {
        _pendingDeleteConfirmId.value = null
        undoDeleteLoad()
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun confirmDeleteLoad() {
        val loadId = _pendingDeleteConfirmId.value ?: _undoDeleteLoadId.value ?: return
        _pendingDeleteConfirmId.value = null
        commitPendingDelete(loadId)
    }

    private fun commitPendingDelete(loadId: String) {
        if (loadId.isBlank()) return
        undoDeleteJob?.cancel()
        undoDeleteJob = null
        if (_undoDeleteLoadId.value == loadId) {
            _undoDeleteLoadId.value = null
        }
        viewModelScope.launch {
            _optimisticOverlay.update { it - loadId }
            _pendingDeleteIds.update { it + loadId }
            try {
                loadRepository.deleteLoad(loadId)
                WidgetDataUpdater.updateWidgetData(app.applicationContext)
                _pendingDeleteIds.update { it - loadId }
            } catch (e: Exception) {
                _pendingDeleteIds.update { it - loadId }
                _swipeSettleGeneration.update { it + 1 }
                _deleteError.value = e.message?.takeIf { it.isNotBlank() }
                    ?: app.getString(R.string.home_delete_failed)
            }
        }
    }

    fun selectWeek(weekStart: String, weekEnd: String, label: String) {
        _uiState.update {
            it.copy(
                filter = LoadFilter.CALENDAR_WEEK,
                selectedWeekStart = weekStart,
                selectedWeekEnd = weekEnd,
                selectedWeekLabel = label,
                selectedDate = null,
                selectedDateLabel = "",
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }
    fun setSearchExpanded(expanded: Boolean) { _uiState.update { it.copy(isSearchExpanded = expanded) } }

    fun refreshBotStatus() {
        viewModelScope.launch {
            val configured = TelegramTokenStore(app).hasToken()
            if (!configured) {
                _uiState.update { it.copy(botStatusActive = false) }
                return@launch
            }
            val token = TelegramTokenStore(app).getToken()
            val health = withContext(Dispatchers.IO) { TelegramBotHealth.check(token) }
            _uiState.update { it.copy(botStatusActive = health.ok) }
        }
    }

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
        _uiState.update {
            it.copy(
                filter = LoadFilter.CALENDAR_DATE,
                selectedDate = date,
                selectedDateLabel = label,
                selectedWeekStart = null,
                selectedWeekEnd = null,
                selectedWeekLabel = "",
            )
        }
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
                formatFilterLabel(app, state.selectedDateLabel, totals.loadCount)
            } else {
                null
            }
            LoadFilter.CALENDAR_WEEK -> if (state.selectedWeekLabel.isNotBlank()) {
                formatFilterLabel(app, state.selectedWeekLabel, totals.loadCount)
            } else {
                null
            }
            LoadFilter.YESTERDAY -> formatFilterLabel(app, app.getString(R.string.home_filter_yesterday), totals.loadCount)
            LoadFilter.THIS_WEEK -> formatFilterLabel(app, app.getString(R.string.home_filter_this_week), totals.loadCount)
            LoadFilter.LAST_WEEK -> formatFilterLabel(app, app.getString(R.string.home_filter_last_week), totals.loadCount)
            LoadFilter.THIS_MONTH -> formatFilterLabel(app, app.getString(R.string.home_filter_this_month), totals.loadCount)
            LoadFilter.DISPUTE -> formatFilterLabel(app, app.getString(R.string.home_filter_dispute), totals.loadCount)
            LoadFilter.ALL -> if (state.selectedYear != null) {
                app.getString(
                    R.string.home_year_selected_header,
                    state.selectedYear ?: Calendar.getInstance().get(Calendar.YEAR),
                    totals.loadCount,
                    loadWord(app, totals.loadCount),
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
        return flattenedListItems(state.filter, state.selectedYear, filteredLoads)
    }
}
