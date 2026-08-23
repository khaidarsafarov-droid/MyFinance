package com.truckerload.presentation.screens.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.truckerload.R
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.OneUiLargeTitleHeader
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.StatsCardSkeleton
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.screens.privacy.PrivacyTrustBadge
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.BentoGlassSearchField
import com.truckerload.presentation.theme.ForestSectionTitle
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.adaptiveLoadColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenContent(
    paddingValues: PaddingValues,
    uiState: HomeUiState,
    searchQuery: String,
    listItems: List<HomeListItem>,
    periodSummary: HomeListItem.FilteredSectionHeader?,
    rpmThresholds: RpmThresholds,
    viewModel: HomeViewModel,
    filteredLoads: List<Load>,
    useRoomPaging: Boolean,
    pagedLoads: LazyPagingItems<Load>,
    onLoadClick: (String) -> Unit,
    context: Context,
    onOpenCalendar: () -> Unit,
    onLoadCamera: (loadId: String, tripId: String, loadDate: String) -> Unit,
    onLoadScan: (loadId: String, tripId: String, loadDate: String) -> Unit,
    onAddLoad: () -> Unit = {},
    onOpenWeeklyGoal: () -> Unit = {},
    onAddDiesel: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    periodTotals: com.truckerload.domain.filter.LoadFilterUseCase.Totals? = null,
    onOpenPrivacy: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val weeklyGoal by LocalWeeklyProfitGoalStore.current.goalAmount.collectAsStateWithLifecycle()
    var showYearSelector by remember { mutableStateOf(false) }
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val swipeSettleGeneration by viewModel.swipeSettleGeneration.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    val tabletChrome = com.truckerload.presentation.utils.useNavigationRail()
    LaunchedEffect(deleteError) {
        val msg = deleteError ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        viewModel.clearDeleteError()
    }

    fun onRefresh() {
        // Do not call pagedLoads.refresh() — a paging refresh can drop itemCount to 0
        // and flash the empty journal. Room flows already update when data changes.
        viewModel.refreshHome()
    }

    fun openArchive() {
        viewModel.setFilter(LoadFilter.ALL)
        showYearSelector = true
    }

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

    if (tabletChrome) {
        val totals = periodTotals ?: periodSummary?.totals
            ?: com.truckerload.domain.filter.LoadFilterUseCase.Totals(0, 0.0, 0.0)
        BentoGlassScreenBackground {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { onRefresh() },
                modifier = Modifier.fillMaxSize(),
                state = rememberPullToRefreshState(),
            ) {
            TabletHomeDashboard(
                paddingValues = paddingValues,
                uiState = uiState,
                searchQuery = searchQuery,
                periodSummary = periodSummary,
                totals = totals,
                recentLoads = filteredLoads,
                viewModel = viewModel,
                onLoadClick = onLoadClick,
                onAddLoad = onAddLoad,
                onOpenWeeklyGoal = onOpenWeeklyGoal,
                onOpenCalendar = onOpenCalendar,
                onOpenArchive = { openArchive() },
            )
            }
        }
        return
    }

    val pullRefreshState = rememberPullToRefreshState()
    val loadColumns = adaptiveLoadColumns()
    val gridRows = remember(listItems, loadColumns) {
        buildHomeGridRows(listItems, loadColumns)
    }

    BentoGlassScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "search") {
                Column {
                    if (tabletChrome) {
                        PrivacyTrustBadge(
                            onClick = onOpenPrivacy,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    if (uiState.isSearchExpanded || searchQuery.isNotBlank()) {
                        BentoGlassSearchField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = stringResource(R.string.home_search_hint),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
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
                        weeklyGoal = weeklyGoal,
                        onOpenWeeklyGoal = onOpenWeeklyGoal,
                    )
                }
            }

            item(key = "newcomer_next") {
                HomeUxMotivators(
                    onAddLoad = onAddLoad,
                    onWeeklyGoal = onOpenWeeklyGoal,
                    onAddDiesel = onAddDiesel,
                    onOpenProfile = onOpenProfile,
                )
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

            if ((useRoomPaging && pagedLoads.itemCount > 0) || (!useRoomPaging && listItems.isNotEmpty())) {
                item(key = "recent_header") {
                    ForestSectionTitle(
                        text = stringResource(R.string.home_recent_loads),
                        modifier = Modifier.padding(horizontal = adaptiveHorizontalPadding()),
                    )
                }
            }

            val visibleItemCount = if (useRoomPaging) pagedLoads.itemCount else listItems.size
            val pagingRefreshLoading = useRoomPaging &&
                pagedLoads.loadState.refresh is LoadState.Loading
            if (HomeRefreshPolicy.shouldShowEmptyJournal(visibleItemCount, pagingRefreshLoading)) {
                item(key = "empty_${uiState.filter}") {
                    HomeEmptyJournal(
                        title = when (uiState.filter) {
                            LoadFilter.ALL -> stringResource(R.string.home_empty_all_title)
                            LoadFilter.CALENDAR_WEEK -> stringResource(R.string.home_empty_calendar_week)
                            LoadFilter.CALENDAR_DATE -> stringResource(R.string.home_empty_calendar_date)
                            LoadFilter.YESTERDAY -> stringResource(R.string.home_empty_yesterday)
                            LoadFilter.THIS_WEEK -> stringResource(R.string.home_empty_this_week)
                            LoadFilter.LAST_WEEK -> stringResource(R.string.home_empty_last_week)
                            LoadFilter.THIS_MONTH -> stringResource(R.string.home_empty_this_month)
                            LoadFilter.DISPUTE -> stringResource(R.string.home_empty_dispute)
                        },
                        body = if (uiState.filter == LoadFilter.THIS_WEEK || uiState.filter == LoadFilter.ALL) {
                            stringResource(R.string.ux_home_empty_reciprocity)
                        } else {
                            stringResource(R.string.home_empty_filtered_body)
                        },
                        ctaLabel = stringResource(R.string.home_empty_cta),
                        onCta = onAddLoad,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else if (useRoomPaging) {
                val rowCount = pagedLoadRowCount(pagedLoads.itemCount, loadColumns)
                items(
                    count = rowCount,
                    key = { rowIndex ->
                        val start = rowIndex * loadColumns
                        val first = pagedLoads.peek(start)
                        "page_row_${first?.id ?: rowIndex}"
                    },
                ) { rowIndex ->
                    val start = rowIndex * loadColumns
                    val rowLoads = (0 until loadColumns).mapNotNull { offset ->
                        pagedLoads[start + offset]
                    }
                    if (rowLoads.isEmpty()) return@items
                    HomeLoadCardRow(
                        loads = rowLoads,
                        columns = loadColumns,
                        rpmThresholds = rpmThresholds,
                        settleKey = swipeSettleGeneration,
                        onLoadClick = onLoadClick,
                        onDelete = { id -> viewModel.requestDeleteLoad(id) },
                        onLoadCamera = onLoadCamera,
                        onLoadScan = onLoadScan,
                    )
                }
            } else {
                itemsIndexed(
                    items = gridRows,
                    key = { _, row ->
                        when (row) {
                            is HomeGridRow.FullWidth -> when (val item = row.item) {
                                is HomeListItem.YearHeader -> "year_${item.section.year}"
                                is HomeListItem.MonthHeader ->
                                    "month_${item.section.year}_${item.section.month}"
                                is HomeListItem.FilteredSectionHeader -> "filtered_${item.label}"
                                is HomeListItem.LoadItem -> "load_${item.load.id}"
                            }
                            is HomeGridRow.Loads ->
                                "loads_${row.loads.joinToString("_") { it.id }}"
                        }
                    },
                ) { _, row ->
                    when (row) {
                        is HomeGridRow.FullWidth -> when (val item = row.item) {
                            is HomeListItem.YearHeader -> YearSectionHeader(section = item.section)
                            is HomeListItem.MonthHeader -> MonthSectionHeader(section = item.section)
                            is HomeListItem.FilteredSectionHeader -> Unit
                            is HomeListItem.LoadItem -> HomeLoadCardRow(
                                loads = listOf(item.load),
                                columns = 1,
                                rpmThresholds = rpmThresholds,
                                settleKey = swipeSettleGeneration,
                                onLoadClick = onLoadClick,
                                onDelete = { id -> viewModel.requestDeleteLoad(id) },
                                onLoadCamera = onLoadCamera,
                                onLoadScan = onLoadScan,
                            )
                        }
                        is HomeGridRow.Loads -> HomeLoadCardRow(
                            loads = row.loads,
                            columns = loadColumns,
                            rpmThresholds = rpmThresholds,
                            settleKey = swipeSettleGeneration,
                            onLoadClick = onLoadClick,
                            onDelete = { id -> viewModel.requestDeleteLoad(id) },
                            onLoadCamera = onLoadCamera,
                            onLoadScan = onLoadScan,
                        )
                    }
                }
            }
        }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenTopBar(
    welcomeName: String,
    weekLabel: String,
    filter: LoadFilter,
    openDrawer: () -> Unit,
    onSearchToggle: () -> Unit,
    onOpenPrivacy: () -> Unit = {},
) {
    val subtitle = buildString {
        if (welcomeName.isNotBlank()) {
            append(stringResource(R.string.home_welcome, welcomeName))
        } else {
            append(stringResource(R.string.app_tagline))
        }
        weekLabel.takeIf { it.isNotBlank() && filter != LoadFilter.THIS_WEEK }?.let { week ->
            append(" · ")
            append(week)
        }
    }
    Column {
        OneUiLargeTitleHeader(
            title = stringResource(R.string.home_brand_title),
            subtitle = subtitle,
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.common_menu),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSearchToggle,
                modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(R.string.home_cd_search),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        )
        PrivacyTrustBadge(
            onClick = onOpenPrivacy,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
    }
}

@Composable
internal fun HomeInitialLoadingOverlay() {
    val tc = LocalTruckColors.current
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
