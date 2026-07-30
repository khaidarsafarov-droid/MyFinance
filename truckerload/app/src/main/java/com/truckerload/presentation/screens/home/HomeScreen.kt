package com.truckerload.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.LoadState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.truckerload.presentation.theme.BentoGlassSearchField
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.theme.UiDimens
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.PeriodFilterStyle
import com.truckerload.presentation.components.LocalOpenDrawer
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.TlOutlinedButton
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.ForestSectionTitle
import com.truckerload.presentation.components.AuthStatusBanner
import com.truckerload.presentation.components.LoadCalendarWithDots
import com.truckerload.presentation.components.StatsCardSkeleton
import com.truckerload.presentation.components.SwipeableLoadCard
import com.truckerload.presentation.connectivity.ConnectivityObserver
import com.truckerload.presentation.connectivity.ConnectivityStatus
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.utils.MoneyFormat
import androidx.compose.foundation.layout.navigationBarsPadding
import com.truckerload.widget.WidgetDeepLink

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onLoadClick: (String) -> Unit,
    onAddLoad: () -> Unit,
    onStats: () -> Unit,
    onWeeklyGoal: () -> Unit = onStats,
    onSettings: () -> Unit = {},
    onLoadCamera: (loadId: String, tripId: String, loadDate: String) -> Unit = { _, _, _ -> },
    onLoadScan: (loadId: String, tripId: String, loadDate: String) -> Unit = { _, _, _ -> },
) {
    val tc = LocalTruckColors.current
    val loadRepository = LocalLoadRepository.current
    val socialProfile by LocalSocialRepository.current.watchMyEnhancedProfile()
        .collectAsStateWithLifecycle(initialValue = null)
    val userProfile by LocalUserProfileStore.current.profile.collectAsStateWithLifecycle()
    val defaultDriverName = stringResource(R.string.default_driver_name)
    val welcomeName = remember(socialProfile, userProfile, defaultDriverName) {
        socialProfile?.displayName
            ?.takeIf { it.isNotBlank() && it !in setOf(defaultDriverName, "Driver", "User") }
            ?: userProfile?.displayName
                ?.takeIf { it.isNotBlank() && it != userProfile?.email }
            ?: ""
    }
    val context = LocalContext.current
    val openDrawer = LocalOpenDrawer.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(loadRepository, isBotConfigured = false, context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredResult by viewModel.filteredLoadsAndTotals.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val connectivity by remember(context) {
        ConnectivityObserver.observe(context)
    }.collectAsStateWithLifecycle(initialValue = ConnectivityStatus.Online)
    val filteredLoads = filteredResult.loads
    val totals = filteredResult.totals
    val datesWithLoads = filteredResult.datesWithLoads
    val useRoomPaging = viewModel.usesRoomPaging(uiState.filter, uiState.selectedYear) ||
        searchQuery.isNotBlank()
    val pagedLoads = viewModel.roomPagedLoads.collectAsLazyPagingItems()
    val weekLabel = remember(
        uiState.filter,
        uiState.selectedDateLabel,
        uiState.selectedWeekLabel,
    ) {
        when (uiState.filter) {
            LoadFilter.CALENDAR_DATE -> uiState.selectedDateLabel
            LoadFilter.CALENDAR_WEEK -> uiState.selectedWeekLabel
            LoadFilter.LAST_WEEK -> {
                val (week, year) = getPreviousWeekNumberAndYear()
                getWeekRange(week, year).third
            }
            else -> ""
        }
    }
    val listItems = remember(
        uiState.filter,
        uiState.selectedYear,
        uiState.selectedDateLabel,
        uiState.selectedWeekLabel,
        filteredLoads,
        totals,
    ) {
        viewModel.flattenedListItems(filteredLoads, totals)
    }
    val periodSummary = remember(
        uiState.filter,
        uiState.selectedYear,
        uiState.selectedDate,
        uiState.selectedWeekStart,
        uiState.selectedWeekEnd,
        uiState.selectedDateLabel,
        uiState.selectedWeekLabel,
        totals.loadCount,
        totals.totalRate,
        totals.totalMiles,
    ) {
        viewModel.periodSummaryHeader(totals)
    }
    val rpmStore = LocalRpmThresholdsStore.current
    val rpmThresholds by rpmStore.thresholds.collectAsStateWithLifecycle()
    var showCalendar by remember { mutableStateOf(false) }
    val cal = remember { java.util.Calendar.getInstance() }
    var calendarYear by remember { mutableStateOf(cal.get(java.util.Calendar.YEAR)) }
    var calendarMonth by remember { mutableStateOf(cal.get(java.util.Calendar.MONTH) + 1) }

    if (showCalendar) {
        Dialog(onDismissRequest = { showCalendar = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = tc.CardBackground,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.home_calendar_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = tc.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showCalendar = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.common_close),
                                tint = tc.TextPrimary,
                            )
                        }
                    }
                    LoadCalendarWithDots(
                        year = calendarYear,
                        month = calendarMonth,
                        datesWithLoads = datesWithLoads,
                        selectedDate = uiState.selectedDate,
                        onDateSelect = { date ->
                            viewModel.selectDate(date)
                            showCalendar = false
                        },
                        onMonthChange = { y, m ->
                            calendarYear = y
                            calendarMonth = m
                        },
                    )
                    TlOutlinedButton(
                        onClick = {
                            val date = uiState.selectedDate
                                ?: java.time.LocalDate.of(
                                    calendarYear,
                                    calendarMonth,
                                    1,
                                ).toString()
                            val (week, year) = com.truckerload.utils.getWeekNumberAndYearFromDate(date)
                            val (start, end, label) = com.truckerload.utils.getWeekRange(week, year)
                            viewModel.selectWeek(start, end, label)
                            showCalendar = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text(
                            stringResource(R.string.home_calendar_select_week),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (WidgetDeepLink.consumeOpenJournalThisWeek(context)) {
            viewModel.setFilter(LoadFilter.THIS_WEEK)
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_brand_title).uppercase(),
                            style = AppTypography.ScreenTitle.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp,
                            ),
                        )
                        if (welcomeName.isNotBlank()) {
                            Text(
                                stringResource(R.string.home_welcome, welcomeName),
                                style = AppTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            )
                        } else {
                            Text(
                                stringResource(R.string.app_tagline),
                                style = AppTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            )
                        }
                        weekLabel.takeIf { it.isNotBlank() && uiState.filter != LoadFilter.THIS_WEEK }?.let { week ->
                            Text(week, style = AppTypography.Caption.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.common_menu),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.setSearchExpanded(!uiState.isSearchExpanded) },
                        modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.home_cd_search),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (connectivity == ConnectivityStatus.Offline) {
                        Text(
                            text = stringResource(R.string.connectivity_offline_banner),
                            color = tc.TextPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tc.AccentExpense.copy(alpha = 0.25f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    AuthStatusBanner()
                    HomeScreenContent(
                        paddingValues = paddingValues,
                        uiState = uiState,
                        searchQuery = searchQuery,
                        listItems = listItems,
                        periodSummary = periodSummary,
                        rpmThresholds = rpmThresholds,
                        datesWithLoads = datesWithLoads,
                        viewModel = viewModel,
                        filteredLoads = filteredLoads,
                        useRoomPaging = useRoomPaging,
                        pagedLoads = pagedLoads,
                        onLoadClick = onLoadClick,
                        onAddLoad = onAddLoad,
                        context = context,
                        onOpenCalendar = { showCalendar = true },
                        onLoadCamera = onLoadCamera,
                        onLoadScan = onLoadScan,
                    )
                }
                if (isInitialLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(tc.Background.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            repeat(3) { index ->
                                StatsCardSkeleton(modifier = Modifier.fillMaxWidth())
                                if (index < 2) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(color = tc.AccentPrimary)
                        }
                    }
                }
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
private fun HomeScreenContent(
    paddingValues: PaddingValues,
    uiState: HomeUiState,
    searchQuery: String,
    listItems: List<HomeListItem>,
    periodSummary: HomeListItem.FilteredSectionHeader?,
    rpmThresholds: RpmThresholds,
    datesWithLoads: Set<String>,
    viewModel: HomeViewModel,
    filteredLoads: List<com.truckerload.domain.model.Load>,
    useRoomPaging: Boolean,
    pagedLoads: androidx.paging.compose.LazyPagingItems<com.truckerload.domain.model.Load>,
    onLoadClick: (String) -> Unit,
    onAddLoad: () -> Unit,
    context: Context,
    onOpenCalendar: () -> Unit,
    onLoadCamera: (loadId: String, tripId: String, loadDate: String) -> Unit,
    onLoadScan: (loadId: String, tripId: String, loadDate: String) -> Unit,
) {
    val tc = LocalTruckColors.current
    var showYearSelector by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pendingDeleteId by viewModel.pendingDeleteConfirmId.collectAsStateWithLifecycle()
    val swipeSettleGeneration by viewModel.swipeSettleGeneration.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    LaunchedEffect(deleteError) {
        val msg = deleteError ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        viewModel.clearDeleteError()
    }

    fun onRefresh() {
        refreshing = true
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.home_sync_triggered),
            android.widget.Toast.LENGTH_SHORT,
        ).show()
        scope.launch {
            delay(800)
            refreshing = false
        }
    }

    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = { onRefresh() })

    if (showYearSelector) {
        AlertDialog(
            onDismissRequest = { showYearSelector = false },
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            title = { Text(stringResource(R.string.home_archive_title), color = tc.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.setSelectedYear(null)
                        showYearSelector = false
                    }) { Text(stringResource(R.string.home_all_years), color = tc.AccentPrimary) }
                    viewModel.availableYears().forEach { year ->
                        TextButton(onClick = {
                            viewModel.setSelectedYear(year)
                            showYearSelector = false
                        }) {
                            Text(
                                stringResource(R.string.home_year_format, year),
                                color = if (uiState.selectedYear == year) tc.AccentPrimary else tc.TextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showYearSelector = false }) { Text(stringResource(R.string.common_close), color = tc.AccentPrimary) } }
        )
    }

    BentoGlassScreenBackground {
        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "search") {
                if (uiState.isSearchExpanded || searchQuery.isNotBlank()) {
                    BentoGlassSearchField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = stringResource(R.string.home_search_hint),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            periodSummary?.let { summary ->
                item(key = "period_summary_${summary.label}") {
                    PeriodSummarySection(
                        header = summary,
                        currentFilter = uiState.filter,
                        selectedYear = uiState.selectedYear,
                        selectedDateLabel = uiState.selectedDateLabel,
                        selectedWeekLabel = uiState.selectedWeekLabel,
                        onFilterSelected = viewModel::setFilter,
                        onOpenCalendar = onOpenCalendar,
                        onOpenArchive = {
                            viewModel.setFilter(LoadFilter.ALL)
                            showYearSelector = true
                        },
                    )
                }
            }

            if (periodSummary == null) {
                item(key = "period_filter") {
                    HomePeriodFilterDropdown(
                        currentFilter = uiState.filter,
                        selectedYear = uiState.selectedYear,
                        selectedDateLabel = uiState.selectedDateLabel,
                        selectedWeekLabel = uiState.selectedWeekLabel,
                        onFilterSelected = viewModel::setFilter,
                        onOpenCalendar = onOpenCalendar,
                        onOpenArchive = {
                            viewModel.setFilter(LoadFilter.ALL)
                            showYearSelector = true
                        },
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            item(key = "add_load_button") {
                TlButton(
                    onClick = onAddLoad,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = adaptiveHorizontalPadding(), vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.home_add_load_button),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_add_load_button),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            if ((useRoomPaging && pagedLoads.itemCount > 0) || (!useRoomPaging && listItems.isNotEmpty())) {
                item(key = "recent_header") {
                    ForestSectionTitle(
                        text = stringResource(R.string.home_recent_loads),
                        modifier = Modifier.padding(horizontal = adaptiveHorizontalPadding()),
                    )
                }
            }

            val pagingEmpty = useRoomPaging &&
                pagedLoads.itemCount == 0 &&
                pagedLoads.loadState.refresh !is LoadState.Loading
            val listEmpty = !useRoomPaging && listItems.isEmpty()
            if (pagingEmpty || listEmpty) {
                item(key = "empty_${uiState.filter}") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when (uiState.filter) {
                                    LoadFilter.ALL -> stringResource(R.string.home_empty_all_title)
                                    LoadFilter.CALENDAR_WEEK -> stringResource(R.string.home_empty_calendar_week)
                                    LoadFilter.CALENDAR_DATE -> stringResource(R.string.home_empty_calendar_date)
                                    LoadFilter.YESTERDAY -> stringResource(R.string.home_empty_yesterday)
                                    LoadFilter.THIS_WEEK -> stringResource(R.string.home_empty_this_week)
                                    LoadFilter.LAST_WEEK -> stringResource(R.string.home_empty_last_week)
                                    LoadFilter.THIS_MONTH -> stringResource(R.string.home_empty_this_month)
                                    LoadFilter.DISPUTE -> stringResource(R.string.home_empty_dispute)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = tc.TextPrimary,
                            )
                            Text(
                                if (uiState.filter == LoadFilter.ALL) {
                                    stringResource(R.string.home_empty_all_body)
                                } else {
                                    stringResource(R.string.home_empty_filtered_body)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            } else if (useRoomPaging) {
                items(
                    count = pagedLoads.itemCount,
                    key = pagedLoads.itemKey { it.id },
                ) { index ->
                    val load = pagedLoads[index] ?: return@items
                    SwipeableLoadCard(
                        load = load,
                        onClick = { if (load.id.isNotBlank()) onLoadClick(load.id) },
                        onDelete = {
                            if (load.id.isNotBlank()) {
                                viewModel.requestDeleteLoad(load.id)
                            }
                        },
                        rpmThresholds = rpmThresholds,
                        modifier = Modifier.padding(horizontal = adaptiveHorizontalPadding()),
                        onCameraClick = {
                            if (load.id.isNotBlank()) {
                                onLoadCamera(load.id, load.tripId, load.date)
                            }
                        },
                        onScanClick = {
                            if (load.id.isNotBlank()) {
                                onLoadScan(load.id, load.tripId, load.date)
                            }
                        },
                        settleKey = swipeSettleGeneration,
                    )
                }
            } else {
                itemsIndexed(
                    items = listItems,
                    key = { _, item ->
                        when (item) {
                            is HomeListItem.YearHeader -> "year_${item.section.year}"
                            is HomeListItem.MonthHeader -> "month_${item.section.year}_${item.section.month}"
                            is HomeListItem.FilteredSectionHeader -> "filtered_${item.label}"
                            is HomeListItem.LoadItem -> "load_${item.load.id}"
                        }
                    },
                ) { _, item ->
                    when (item) {
                        is HomeListItem.YearHeader -> YearSectionHeader(section = item.section)
                        is HomeListItem.MonthHeader -> MonthSectionHeader(section = item.section)
                        is HomeListItem.FilteredSectionHeader -> Unit
                        is HomeListItem.LoadItem -> SwipeableLoadCard(
                            load = item.load,
                            onClick = { if (item.load.id.isNotBlank()) onLoadClick(item.load.id) },
                            onDelete = {
                                if (item.load.id.isNotBlank()) {
                                    viewModel.requestDeleteLoad(item.load.id)
                                }
                            },
                            rpmThresholds = rpmThresholds,
                            modifier = Modifier.padding(horizontal = adaptiveHorizontalPadding()),
                            onCameraClick = {
                                if (item.load.id.isNotBlank()) {
                                    onLoadCamera(item.load.id, item.load.tripId, item.load.date)
                                }
                            },
                            onScanClick = {
                                if (item.load.id.isNotBlank()) {
                                    onLoadScan(item.load.id, item.load.tripId, item.load.date)
                                }
                            },
                            settleKey = swipeSettleGeneration,
                        )
                    }
                }
            }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteLoad,
            title = { Text(stringResource(R.string.load_delete_confirm_title)) },
            text = { Text(stringResource(R.string.load_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteLoad) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteLoad) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
    }
}
