package com.truckerload.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.truckerload.BuildConfig
import com.truckerload.presentation.components.BotStatusBadge
import com.truckerload.presentation.components.LoadCard
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.screens.home.HomeListItem
import com.truckerload.sync.TelegramSyncWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLoadClick: (String) -> Unit,
    onAddLoad: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    val tc = LocalTruckColors.current
    val loadRepository = LocalLoadRepository.current
    val context = LocalContext.current
    val isBotConfigured = BuildConfig.TELEGRAM_BOT_TOKEN.isNotEmpty() && BuildConfig.GEMINI_API_KEY.isNotEmpty()
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(loadRepository, isBotConfigured))
    val uiState by viewModel.uiState.collectAsState()
    val listItems = viewModel.flattenedListItems()

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
                            }
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync from Telegram")
                        }
                    }
                    IconButton(onClick = { viewModel.setSearchExpanded(!uiState.isSearchExpanded) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                Icon(Icons.Default.Add, contentDescription = "Add load")
            }
        },
        content = { paddingValues -> HomeScreenContent(paddingValues, uiState, listItems, viewModel, onLoadClick) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    paddingValues: PaddingValues,
    uiState: HomeUiState,
    listItems: List<HomeListItem>,
    viewModel: HomeViewModel,
    onLoadClick: (String) -> Unit
) {
    val tc = LocalTruckColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    val cal = remember { java.util.Calendar.getInstance() }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = cal.timeInMillis,
            yearRange = IntRange(cal.get(java.util.Calendar.YEAR) - 2, cal.get(java.util.Calendar.YEAR) + 1)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { ms ->
                        viewModel.selectDateFromCalendar(ms)
                    }
                    showDatePicker = false
                }) { Text("Выбрать") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) { DatePicker(state = dateState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (uiState.isSearchExpanded) {
            BasicTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(tc.SurfaceSecondary),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box(Modifier.padding(12.dp)) {
                        if (uiState.searchQuery.isEmpty()) {
                            Text("Trip ID, город, дата (25.10.2023)...", color = tc.TextSecondary)
                        }
                        inner()
                    }
                }
            )
        }
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.filter == LoadFilter.CALENDAR_WEEK,
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            if (uiState.filter == LoadFilter.CALENDAR_WEEK && uiState.selectedWeekLabel.isNotBlank())
                                uiState.selectedWeekLabel
                            else
                                "Календарь"
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tc.SurfaceSecondary,
                        selectedLabelColor = tc.TextPrimary,
                        containerColor = tc.CardBackground,
                        labelColor = tc.TextSecondary
                    )
                )
            }
            item {
                FilterChip(
                    selected = uiState.filter == LoadFilter.ALL,
                    onClick = { viewModel.setFilter(LoadFilter.ALL) },
                    label = { Text("Архив по годам") },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tc.SurfaceSecondary,
                        selectedLabelColor = tc.TextPrimary,
                        containerColor = tc.CardBackground,
                        labelColor = tc.TextSecondary
                    )
                )
            }
            item {
                FilterChip(
                    selected = uiState.filter == LoadFilter.YESTERDAY,
                    onClick = { viewModel.setFilter(LoadFilter.YESTERDAY) },
                    label = { Text("вчера") },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tc.SurfaceSecondary,
                        selectedLabelColor = tc.TextPrimary,
                        containerColor = tc.CardBackground,
                        labelColor = tc.TextSecondary
                    )
                )
            }
            item {
                FilterChip(
                    selected = uiState.filter == LoadFilter.THIS_WEEK,
                    onClick = { viewModel.setFilter(LoadFilter.THIS_WEEK) },
                    label = { Text("эта неделя") },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tc.SurfaceSecondary,
                        selectedLabelColor = tc.TextPrimary,
                        containerColor = tc.CardBackground,
                        labelColor = tc.TextSecondary
                    )
                )
            }
            item {
                FilterChip(
                    selected = uiState.filter == LoadFilter.LAST_WEEK,
                    onClick = { viewModel.setFilter(LoadFilter.LAST_WEEK) },
                    label = { Text("прошлая неделя") },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tc.SurfaceSecondary,
                        selectedLabelColor = tc.TextPrimary,
                        containerColor = tc.CardBackground,
                        labelColor = tc.TextSecondary
                    )
                )
            }
            item {
                FilterChip(
                    selected = uiState.filter == LoadFilter.THIS_MONTH,
                    onClick = { viewModel.setFilter(LoadFilter.THIS_MONTH) },
                    label = { Text("этот месяц") },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tc.SurfaceSecondary,
                        selectedLabelColor = tc.TextPrimary,
                        containerColor = tc.CardBackground,
                        labelColor = tc.TextSecondary
                    )
                )
            }
        }
        if (listItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (uiState.filter) {
                            LoadFilter.ALL -> "Нет грузов"
                            LoadFilter.CALENDAR_WEEK -> "Нет грузов за выбранную неделю"
                            LoadFilter.YESTERDAY -> "Нет грузов за вчера"
                            LoadFilter.THIS_WEEK -> "Нет грузов за эту неделю"
                            LoadFilter.LAST_WEEK -> "Нет грузов за прошлую неделю"
                            LoadFilter.THIS_MONTH -> "Нет грузов за этот месяц"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (uiState.filter == LoadFilter.ALL)
                            "Отправьте данные боту в Telegram или добавьте груз вручную"
                        else
                            "Смените фильтр или добавьте груз",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = listItems,
                    key = { index, item ->
                        when (item) {
                            is HomeListItem.YearHeader -> "year_${item.section.year}"
                            is HomeListItem.MonthHeader -> "month_${item.section.year}_${item.section.month}"
                            is HomeListItem.LoadItem -> "load_${item.load.id}"
                        }
                    }
                ) { _, item ->
                    when (item) {
                        is HomeListItem.YearHeader -> YearSectionHeader(section = item.section)
                        is HomeListItem.MonthHeader -> MonthSectionHeader(section = item.section)
                        is HomeListItem.LoadItem -> LoadCard(
                            load = item.load,
                            onClick = { if (item.load.id.isNotBlank()) onLoadClick(item.load.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearSectionHeader(section: YearSection) {
    val tc = LocalTruckColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Text(
            text = "${section.year} год",
            style = MaterialTheme.typography.titleLarge,
            color = tc.AccentPrimary
        )
        Text(
            text = "Всего ${section.loadCount} грузов  •  $${String.format("%,.0f", section.totalRate)}  •  ${String.format("%,.0f", section.totalMiles)} mi",
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
        text = "${section.monthName} (${section.loads.size} грузов)",
        style = MaterialTheme.typography.titleSmall,
        color = tc.TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp, start = 8.dp)
    )
}
