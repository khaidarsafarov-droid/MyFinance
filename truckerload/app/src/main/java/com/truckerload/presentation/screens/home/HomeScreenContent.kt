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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.truckerload.R
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.StatsCardSkeleton
import com.truckerload.presentation.components.SwipeableLoadCard
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.BentoGlassSearchField
import com.truckerload.presentation.theme.ForestSectionTitle
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val pullRefreshState = rememberPullToRefreshState()

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
        Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
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
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenTopBar(
    welcomeName: String,
    weekLabel: String,
    filter: LoadFilter,
    openDrawer: () -> Unit,
    onSearchToggle: () -> Unit,
) {
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
                weekLabel.takeIf { it.isNotBlank() && filter != LoadFilter.THIS_WEEK }?.let { week ->
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
                onClick = onSearchToggle,
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
