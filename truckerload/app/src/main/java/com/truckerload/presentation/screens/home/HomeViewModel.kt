package com.truckerload.presentation.screens.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.sync.TelegramBotPoller
import com.truckerload.sync.TelegramSyncWorker
import com.truckerload.widget.WidgetDataUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
@Suppress("ktlint:standard:function-signature", "ktlint:standard:class-signature")
class HomeViewModel @Inject constructor(
    private val loadRepository: LoadRepository,
    private val app: Application,
) : ViewModel() {

    companion object {
        /** Foreground-сервис бота запускаем один раз за процесс, не при каждом recreate VM. */
        private val botServiceStarted = AtomicBoolean(false)

        /**
         * Keep the last Room snapshot after subscribers leave so returning to Home
         * (or a pull-to-refresh resubscribe) does not flash empty totals.
         */
        private val homeSharing = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = 5_000,
            replayExpirationMillis = Long.MAX_VALUE,
        )
    }

    private val filterUseCase = LoadFilterUseCase()
    private val botStatus = HomeBotStatus(app)

    /** Survives onCleared. viewModelScope is already cancelled there, and runBlocking would ANR. */
    private val deleteScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Room subscription scoped by filter.
     * Week / ALL / DISPUTE journals use [roomPagedLoads] + SQL totals — avoid dual hydrate.
     * Day / month load only the overlapping PU–DEL window (not the whole fleet).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val loadsFromDb: StateFlow<List<Load>> = _uiState
        .map { Triple(it.filter, it.selectedDate, it.selectedMonthYear to it.selectedMonth) }
        .distinctUntilChanged()
        .flatMapLatest { (filter, selectedDate, month) ->
            val (monthYear, monthNum) = month
            HomeScopedLoadQuery.observe(
                filter = filter,
                selectedDate = selectedDate,
                selectedMonthYear = monthYear,
                selectedMonth = monthNum,
                loadRepository = loadRepository,
            )
        }
        .conflate()
        .stateIn(
            scope = viewModelScope,
            started = homeSharing,
            initialValue = emptyList(),
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Room can emit an empty list for a beat while pull-to-refresh invalidates.
     * Keep the last populated snapshot so the hero and journal do not flash zero.
     */
    private val displayedLoadsFromDb: StateFlow<List<Load>> = combine(
        loadsFromDb,
        _isRefreshing,
    ) { loads, refreshing -> loads to refreshing }
        .scan(emptyList<Load>()) { previous, (loads, refreshing) ->
            HomeRefreshPolicy.retainLoads(loads, previous, refreshing)
        }
        .drop(1)
        .stateIn(
            scope = viewModelScope,
            started = homeSharing,
            initialValue = emptyList(),
        )

    private val initialLoadDone = MutableStateFlow(false)

    /** true до первого эмита из Room. */
    val isInitialLoading: StateFlow<Boolean> = initialLoadDone
        .map { done -> !done }
        .stateIn(
            scope = viewModelScope,
            started = homeSharing,
            initialValue = true,
        )

    /** Immediate field text. Lists read [debouncedSearchQuery] only. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val debouncedSearchQuery: StateFlow<String> = _searchQuery
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
                    selectedMonthYear = state.selectedMonthYear,
                    selectedMonth = state.selectedMonth,
                    selectedMonthLabel = state.selectedMonthLabel,
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
    private val optimisticOverlay = MutableStateFlow<Map<String, Load>>(emptyMap())

    private val deletes = HomeDeleteCoordinator(
        app = app,
        loadRepository = loadRepository,
        scope = viewModelScope,
        commitScope = deleteScope,
        optimisticOverlay = optimisticOverlay,
        tripIdOf = { id ->
            displayedLoadsFromDb.value.firstOrNull { it.id == id }?.tripId
                ?: optimisticOverlay.value[id]?.tripId
        },
    )
    val pendingDeleteConfirmId: StateFlow<String?> = deletes.pendingDeleteConfirmId
    val undoDeleteLoadId: StateFlow<String?> = deletes.undoDeleteLoadId
    val deleteError: StateFlow<String?> = deletes.deleteError
    val swipeSettleGeneration: StateFlow<Int> = deletes.swipeSettleGeneration

    /** SQL totals for ALL filter — avoids hydrating the full journal for header stats. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val allFilterTotals: StateFlow<LoadFilterUseCase.Totals?> = filterState
        .flatMapLatest { state ->
            if (state.filter != LoadFilter.ALL) {
                flowOf(null)
            } else {
                loadRepository.watchJournalTotals(state.selectedYear)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = homeSharing,
            initialValue = null,
        )

    /**
     * SQL totals for Room-paged journals (week / dispute) — avoids dual hydrate
     * while keeping header numbers accurate.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val pagedFilterTotals: StateFlow<LoadFilterUseCase.Totals?> =
        combine(filterState, WeekStartRuntime.revision) { state, _ -> state }
            .flatMapLatest { state -> HomePagedFilterTotals.observe(state, loadRepository) }
            .stateIn(
                scope = viewModelScope,
                started = homeSharing,
                initialValue = null,
            )

    private val archiveYears = MutableStateFlow<List<Int>>(emptyList())

    /** Фильтрованный список + итоги. Calendar dots live in [calendarDatesWithLoads]. */
    val filteredLoadsAndTotals: StateFlow<FilteredResult> = combine(
        combine(
            displayedLoadsFromDb,
            optimisticOverlay,
            deletes.pendingDeleteIds,
            filterState,
            allFilterTotals,
        ) { loads, overlay, pendingDeletes, filter, sqlTotals ->
            HomeFilterInputs(loads, overlay, pendingDeletes, filter, sqlTotals, pagedTotals = null)
        },
        pagedFilterTotals,
    ) { inputs, pagedTotals ->
        HomeFilteredResultBuilder.build(inputs.copy(pagedTotals = pagedTotals), filterUseCase)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = homeSharing,
            initialValue = HomeFilteredResultBuilder.EMPTY,
        )

    /**
     * Dates that have loads — collected only while the calendar dialog is composed so
     * opening Home does not hydrate the full journal into memory.
     */
    val calendarDatesWithLoads: StateFlow<Set<String>> = loadRepository.watchLoadDateSpans()
        .map { HomeCalendarDateSpans.toDateKeys(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomPagedLoads: Flow<PagingData<Load>> =
        combine(filterState, WeekStartRuntime.revision) { filter, _ -> filter }
            .flatMapLatest { filter -> HomeRoomPagingFlow.observe(filter, loadRepository) }
            .cachedIn(viewModelScope)

    fun usesRoomPaging(filter: LoadFilter, year: Int?) = HomeRoomPagingPolicy.usesRoomPaging(filter, year)

    init {
        viewModelScope.launch {
            runCatching { archiveYears.value = loadRepository.getDistinctLoadYears() }
        }
        viewModelScope.launch {
            displayedLoadsFromDb.collect { list ->
                if (!initialLoadDone.value) {
                    initialLoadDone.value = true
                }
                _uiState.update { it.copy(loads = list) }
                val ids = list.map { it.id }.toSet()
                // Keep only optimistic inserts not yet present in Room.
                optimisticOverlay.update { current -> current.filterKeys { it !in ids } }
                // Once Room confirms deletion, clear pending delete markers.
                deletes.onRoomLoads(ids)
            }
        }
        viewModelScope.launch { startBotPollerWhenActive() }
    }

    private suspend fun startBotPollerWhenActive() {
        runCatching {
            val state = botStatus.check()
            if (state == HomeBotStatus.State.NOT_CONFIGURED) return@runCatching
            val active = state == HomeBotStatus.State.ACTIVE
            _uiState.update { it.copy(botStatusActive = active) }
            if (active && botServiceStarted.compareAndSet(false, true)) {
                TelegramBotPoller.start(app)
            }
        }
    }

    /** Оптимистичное обновление: сразу отобразить изменения в UI до сохранения в БД. */
    fun applyOptimisticUpdate(load: Load) {
        optimisticOverlay.update { it + (load.id to load) }
    }

    /** Откат оптимистичного обновления при ошибке сохранения. */
    fun revertOptimisticUpdate(loadId: String) {
        optimisticOverlay.update { it - loadId }
    }

    fun deleteLoad(loadId: String) = deletes.request(loadId)

    /**
     * Soft-hides the load immediately and starts an Undo window before hard delete.
     * Keeps confirm APIs for callers that still use the dialog path.
     */
    fun requestDeleteLoad(loadId: String) = deletes.request(loadId)

    fun undoDeleteLoad() = deletes.undo()

    fun dismissDeleteLoad() = deletes.dismiss()

    fun clearDeleteError() = deletes.clearError()

    fun confirmDeleteLoad() = deletes.confirm()

    /** Leaving Home / process death: hard-delete so the load cannot come back. */
    fun commitPendingDeletesNow() = deletes.commitAllNow()

    fun selectWeek(weekStart: String, weekEnd: String, label: String) {
        _uiState.update { it.withCalendarWeek(weekStart, weekEnd, label) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isSearchExpanded = expanded) }
    }

    fun refreshHome() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                runCatching { refreshBotStatusSuspend() }
                runCatching { WidgetDataUpdater.updateWidgetData(app) }
                runCatching { TelegramSyncWorker.enqueueEnsureService(app, replace = true) }
                delay(HomeRefreshPolicy.MIN_INDICATOR_MS)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshBotStatus() {
        viewModelScope.launch { refreshBotStatusSuspend() }
    }

    private suspend fun refreshBotStatusSuspend() {
        val active = botStatus.check() == HomeBotStatus.State.ACTIVE
        _uiState.update { it.copy(botStatusActive = active) }
    }

    fun setFilter(filter: LoadFilter) {
        _uiState.update { it.withFilter(filter) }
    }

    /** Whole calendar month totals (any past/current month). */
    fun selectMonth(year: Int, month: Int) {
        _uiState.update { it.withWholeMonth(year, month) }
    }

    /** Trucking week inside a previously chosen calendar month. */
    fun selectMonthWeek(year: Int, month: Int, weekNumber: Int, weekYear: Int) {
        _uiState.update { it.withMonthWeek(year, month, weekNumber, weekYear) }
    }

    fun setSelectedYear(year: Int?) {
        _uiState.update { it.withArchiveYear(year) }
    }

    fun selectDate(date: String) {
        val label = formatDateLabel(date)
        _uiState.update { it.withCalendarDate(date, label) }
    }

    fun selectDateFromCalendar(dateMillis: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        selectDate("%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)))
    }

    /** Список годов с грузами для селектора архива (SQL, не filter-scoped). */
    fun availableYears(): List<Int> = archiveYears.value
        .ifEmpty { listOf(Calendar.getInstance().get(Calendar.YEAR)) }

    /** Заголовок с итогами выбранного периода — показывается над фильтром на главном экране. */
    fun periodSummaryHeader(totals: LoadFilterUseCase.Totals): HomeListItem.FilteredSectionHeader? =
        buildPeriodSummaryHeader(_uiState.value, totals, app)

    /** Плоский список для LazyColumn. */
    fun flattenedListItems(filteredLoads: List<Load>, totals: LoadFilterUseCase.Totals): List<HomeListItem> {
        val state = _uiState.value
        return flattenedListItems(state.filter, state.selectedYear, filteredLoads)
    }

    override fun onCleared() {
        commitPendingDeletesNow()
        super.onCleared()
    }
}
