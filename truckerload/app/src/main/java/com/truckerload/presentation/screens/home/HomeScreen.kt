package com.truckerload.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.BentoGlassSearchField
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.R
import com.truckerload.presentation.components.BotStatusBadge
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.presentation.theme.StaggeredAnimatedItem
import com.truckerload.presentation.components.BentoSectionTitle
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.LoadCalendarWithDots
import com.truckerload.presentation.components.RpmColorLegend
import com.truckerload.presentation.components.SwipeableLoadCard
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.sync.TelegramSyncWorker
import com.truckerload.widget.WidgetDeepLink

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onLoadClick: (String) -> Unit,
    onAddLoad: () -> Unit,
    onStats: () -> Unit,
    onWeeklyGoal: () -> Unit = onStats,
    onSettings: () -> Unit = {}
) {
    val tc = LocalTruckColors.current
    val loadRepository = LocalLoadRepository.current
    val context = LocalContext.current
    val isBotConfigured = remember(context) { TelegramTokenStore(context).hasToken() }
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(loadRepository, isBotConfigured, context))
    val uiState by viewModel.uiState.collectAsState()
    val filteredResult by viewModel.filteredLoadsAndTotals.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val filteredLoads = filteredResult.loads
    val totals = filteredResult.totals
    val datesWithLoads = filteredResult.datesWithLoads
    val weekLabel = remember {
        val (week, year) = getCurrentWeekNumberAndYear()
        getWeekRange(week, year).third
    }
    val listItems = remember(uiState, filteredLoads, totals) {
        viewModel.flattenedListItems(filteredLoads, totals)
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
                            stringResource(R.string.home_brand_title),
                            color = tc.AccentPrimary,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = tc.TextSecondary
                        )
                        weekLabel.takeIf { it.isNotBlank() }?.let { week ->
                            Text(
                                week,
                                style = MaterialTheme.typography.labelSmall,
                                color = tc.TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                    actionIconContentColor = tc.TextPrimary
                ),
                actions = {
                    if (isBotConfigured) {
                        IconButton(
                            onClick = {
                                val work = OneTimeWorkRequestBuilder<TelegramSyncWorker>().build()
                                WorkManager.getInstance(context.applicationContext).enqueue(work)
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                        Icon(Icons.Default.Sync, contentDescription = stringResource(R.string.home_cd_sync_telegram))
                        }
                    }
                    IconButton(onClick = { viewModel.setSearchExpanded(!uiState.isSearchExpanded) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.home_cd_search))
                    }
                    IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.home_cd_settings))
                    }
                    IconButton(onClick = onStats, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.BarChart, contentDescription = stringResource(R.string.home_cd_statistics))
                    }
                    BotStatusBadge(active = uiState.botStatusActive)
                }
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreenContent(
                    paddingValues = paddingValues,
                    uiState = uiState,
                    listItems = listItems,
                    totals = totals,
                    datesWithLoads = datesWithLoads,
                    viewModel = viewModel,
                    filteredLoads = filteredLoads,
                    onLoadClick = onLoadClick,
                    onAddLoad = onAddLoad,
                    context = context
                )
                if (isInitialLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(tc.Background.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = tc.AccentPrimary)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
private fun HomeScreenContent(
    paddingValues: PaddingValues,
    uiState: HomeUiState,
    listItems: List<HomeListItem>,
    totals: LoadFilterUseCase.Totals,
    datesWithLoads: Set<String>,
    viewModel: HomeViewModel,
    filteredLoads: List<com.truckerload.domain.model.Load>,
    onLoadClick: (String) -> Unit,
    onAddLoad: () -> Unit,
    context: Context
) {
    val tc = LocalTruckColors.current
    var showCalendar by remember { mutableStateOf(false) }
    var showYearSelector by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun onRefresh() {
        refreshing = true
        val work = OneTimeWorkRequestBuilder<TelegramSyncWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueue(work)
        scope.launch {
            delay(2500)
            refreshing = false
        }
    }

    val cal = remember { java.util.Calendar.getInstance() }
    var calendarYear by remember { mutableStateOf(cal.get(java.util.Calendar.YEAR)) }
    var calendarMonth by remember { mutableStateOf(cal.get(java.util.Calendar.MONTH) + 1) }

    if (showCalendar) {
        AlertDialog(
            onDismissRequest = { showCalendar = false },
            containerColor = tc.CardBackground,
            titleContentColor = tc.TextPrimary,
            textContentColor = tc.TextPrimary,
            title = { Text(stringResource(R.string.home_calendar_title), color = tc.TextPrimary) },
            text = {
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
                    }
                )
            },
            confirmButton = { TextButton(onClick = { showCalendar = false }) { Text(stringResource(R.string.common_close), color = tc.AccentPrimary) } }
        )
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

    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = { onRefresh() })
    BentoGlassScreenBackground {
    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isSearchExpanded) {
                BentoGlassSearchField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = stringResource(R.string.home_search_hint),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            HomeBentoDashboard(
                totals = totals,
            )

            HomePeriodFilterDropdown(
                currentFilter = uiState.filter,
                selectedYear = uiState.selectedYear,
                selectedDateLabel = uiState.selectedDateLabel,
                selectedWeekLabel = uiState.selectedWeekLabel,
                onFilterSelected = viewModel::setFilter,
                onOpenCalendar = { showCalendar = true },
                onOpenArchive = {
                    viewModel.setFilter(LoadFilter.ALL)
                    showYearSelector = true
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (listItems.isNotEmpty()) {
                    item(key = "recent_header") {
                        StaggeredAnimatedItem(index = 0) {
                            BentoSectionTitle(
                                title = stringResource(R.string.home_recent_loads),
                                emoji = "📋"
                            )
                        }
                    }
                }
                if (listItems.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp),
                            contentAlignment = Alignment.Center
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
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tc.TextPrimary
                                )
                                Text(
                                    if (uiState.filter == LoadFilter.ALL)
                                        stringResource(R.string.home_empty_all_body)
                                    else stringResource(R.string.home_empty_filtered_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tc.TextSecondary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = listItems,
                        key = { index, item ->
                            when (item) {
                                is HomeListItem.YearHeader -> "year_${item.section.year}"
                                is HomeListItem.MonthHeader -> "month_${item.section.year}_${item.section.month}"
                                is HomeListItem.FilteredSectionHeader -> "filtered_${item.label}"
                                is HomeListItem.LoadItem -> "load_${item.load.id}"
                            }
                        }
                    ) { index, item ->
                        StaggeredAnimatedItem(index = index + 1) {
                            when (item) {
                                is HomeListItem.YearHeader -> YearSectionHeader(section = item.section)
                                is HomeListItem.MonthHeader -> MonthSectionHeader(section = item.section)
                                is HomeListItem.FilteredSectionHeader -> FilteredSectionHeader(header = item)
                                is HomeListItem.LoadItem -> SwipeableLoadCard(
                                    load = item.load,
                                    onClick = { if (item.load.id.isNotBlank()) onLoadClick(item.load.id) },
                                    onDelete = {
                                        if (item.load.id.isNotBlank()) {
                                            viewModel.deleteLoad(item.load.id)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                item(key = "add_load_button") {
                    Button(
                        onClick = onAddLoad,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tc.AccentPrimary,
                            contentColor = tc.Background
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_add_load_button),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
    }
}

@Composable
private fun HomeBentoDashboard(
    totals: LoadFilterUseCase.Totals,
) {
    val tc = LocalTruckColors.current
    StatsHeader(totals = totals, tc = tc)
}

@Composable
private fun StatsHeader(totals: LoadFilterUseCase.Totals, tc: com.truckerload.presentation.theme.TruckColorPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.metric_loads),
                value = totals.loadCount.toString(),
                accent = FinanceCockpitColors.SalaryAccent
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.metric_gross),
                value = String.format("$%,.0f", totals.totalRate),
                accent = BentoGlassTheme.GoalGradientEnd,
                highlight = true
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.widget_metric_miles),
                value = String.format("%,.0f", totals.totalMiles),
                accent = FinanceCockpitColors.TextPrimary
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.widget_metric_cpm),
                value = totals.avgRpmFormatted.substringBefore(" /").ifBlank { "—" },
                accent = tc.AccentInfo
            )
        }
        RpmColorLegend(
            compact = true,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun FilteredSectionHeader(header: HomeListItem.FilteredSectionHeader) {
    val tc = LocalTruckColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Text(
            text = header.label,
            style = MaterialTheme.typography.titleMedium,
            color = tc.TextPrimary
        )
        Text(
            text = stringResource(
                R.string.home_filtered_header,
                header.totals.totalRate,
                header.totals.totalMiles,
                header.totals.avgRpmFormatted
            ),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun YearSectionHeader(section: YearSection) {
    val tc = LocalTruckColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp)) {
        Text(text = stringResource(R.string.home_year_section, section.year), style = MaterialTheme.typography.titleLarge, color = tc.AccentPrimary)
        Text(
            text = stringResource(
                R.string.home_year_totals,
                section.loadCount,
                section.totalRate,
                section.totalMiles
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun MonthSectionHeader(section: MonthSection) {
    val tc = LocalTruckColors.current
    Text(
        text = stringResource(R.string.home_month_section, section.monthName, section.loads.size),
        style = MaterialTheme.typography.titleSmall,
        color = tc.TextPrimary,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, start = 8.dp)
    )
}
