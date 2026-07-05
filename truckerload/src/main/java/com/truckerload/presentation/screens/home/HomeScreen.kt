package com.truckerload.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.LocalTruckColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.presentation.components.BotStatusBadge
import com.truckerload.presentation.components.LoadCalendarWithDots
import com.truckerload.presentation.components.LoadCard
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.sync.TelegramSyncWorker

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onLoadClick: (String) -> Unit,
    onAddLoad: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val tc = LocalTruckColors.current
    val loadRepository = LocalLoadRepository.current
    val context = LocalContext.current
    val isBotConfigured = BuildConfig.TELEGRAM_BOT_TOKEN.isNotEmpty() && BuildConfig.CEREBRAS_API_KEY.isNotEmpty()
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(loadRepository, isBotConfigured, context))
    val uiState by viewModel.uiState.collectAsState()
    val filteredResult by viewModel.filteredLoadsAndTotals.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val filteredLoads = filteredResult.loads
    val totals = filteredResult.totals
    val datesWithLoads = filteredResult.datesWithLoads
    val listItems = remember(uiState, filteredLoads, totals) {
        viewModel.flattenedListItems(filteredLoads, totals)
    }

    LaunchedEffect(Unit) { viewModel.setFilter(LoadFilter.ALL) }

    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text("TruckerLoad", color = tc.TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
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
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_cd_settings))
                    }
                    IconButton(onClick = onStats, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.home_cd_statistics))
                    }
                    BotStatusBadge(active = uiState.botStatusActive)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLoad,
                containerColor = tc.AccentPrimary,
                contentColor = tc.Background
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_cd_add_load))
            }
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
                    onLoadClick = onLoadClick,
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
    onLoadClick: (String) -> Unit,
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
    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isSearchExpanded) {
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(tc.CardBackground, RoundedCornerShape(12.dp)),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = tc.TextPrimary),
                    cursorBrush = SolidColor(tc.AccentPrimary),
                    decorationBox = { inner ->
                        Box(Modifier.padding(12.dp)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(stringResource(R.string.home_search_hint), color = tc.TextSecondary)
                            }
                            inner()
                        }
                    }
                )
            }

            // Stats header — динамически пересчитывается
            StatsHeader(totals = totals, tc = tc)

            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.filter == LoadFilter.CALENDAR_WEEK || uiState.filter == LoadFilter.CALENDAR_DATE,
                        onClick = { showCalendar = true },
                        label = {
                            Text(
                                when {
                                    uiState.filter == LoadFilter.CALENDAR_DATE && uiState.selectedDateLabel.isNotBlank() ->
                                        uiState.selectedDateLabel
                                    uiState.filter == LoadFilter.CALENDAR_WEEK && uiState.selectedWeekLabel.isNotBlank() ->
                                        uiState.selectedWeekLabel
                                    else -> stringResource(R.string.home_filter_calendar)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = tc.AccentPrimary,
                            containerColor = tc.CardBackground,
                            labelColor = tc.TextSecondary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter == LoadFilter.ALL,
                        onClick = {
                            if (uiState.filter == LoadFilter.ALL) showYearSelector = true
                            else viewModel.setFilter(LoadFilter.ALL)
                        },
                        label = {
                            Text(
                                when {
                                    uiState.selectedYear != null -> stringResource(R.string.home_year_format, uiState.selectedYear!!)
                                    else -> stringResource(R.string.home_filter_archive)
                                }
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = tc.AccentPrimary,
                            containerColor = tc.CardBackground,
                            labelColor = tc.TextSecondary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter == LoadFilter.YESTERDAY,
                        onClick = { viewModel.setFilter(LoadFilter.YESTERDAY) },
                        label = { Text(stringResource(R.string.home_filter_yesterday)) },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = tc.AccentPrimary,
                            containerColor = tc.CardBackground,
                            labelColor = tc.TextSecondary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter == LoadFilter.THIS_WEEK,
                        onClick = { viewModel.setFilter(LoadFilter.THIS_WEEK) },
                        label = { Text(stringResource(R.string.home_filter_this_week)) },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = tc.AccentPrimary,
                            containerColor = tc.CardBackground,
                            labelColor = tc.TextSecondary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter == LoadFilter.LAST_WEEK,
                        onClick = { viewModel.setFilter(LoadFilter.LAST_WEEK) },
                        label = { Text(stringResource(R.string.home_filter_last_week)) },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = tc.AccentPrimary,
                            containerColor = tc.CardBackground,
                            labelColor = tc.TextSecondary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter == LoadFilter.THIS_MONTH,
                        onClick = { viewModel.setFilter(LoadFilter.THIS_MONTH) },
                        label = { Text(stringResource(R.string.home_filter_this_month)) },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = tc.AccentPrimary,
                            containerColor = tc.CardBackground,
                            labelColor = tc.TextSecondary
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    ) { _, item ->
                        when (item) {
                            is HomeListItem.YearHeader -> YearSectionHeader(section = item.section)
                            is HomeListItem.MonthHeader -> MonthSectionHeader(section = item.section)
                            is HomeListItem.FilteredSectionHeader -> FilteredSectionHeader(header = item)
                            is HomeListItem.LoadItem -> LoadCard(
                                load = item.load,
                                onClick = { if (item.load.id.isNotBlank()) onLoadClick(item.load.id) }
                            )
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun StatsHeader(totals: LoadFilterUseCase.Totals, tc: com.truckerload.presentation.theme.TruckColorPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(
                R.string.home_stats_header,
                totals.loadCount,
                totals.totalRate,
                totals.totalMiles,
                totals.avgRpmFormatted
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary
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
