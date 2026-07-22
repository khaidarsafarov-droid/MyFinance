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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.truckerload.presentation.theme.BentoGlassSearchField
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.theme.UiDimens
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import com.truckerload.presentation.components.LocalOpenDrawer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import com.truckerload.presentation.components.TlButton
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
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.R
import com.truckerload.presentation.components.BotStatusBadge
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.DarkGlassScreenTitle
import com.truckerload.presentation.theme.DarkGlassSectionTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.LoadCalendarWithDots
import com.truckerload.presentation.components.SwipeableLoadCard
import com.truckerload.presentation.connectivity.ConnectivityObserver
import com.truckerload.presentation.connectivity.ConnectivityStatus
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.sync.TelegramSyncWorker
import com.truckerload.presentation.utils.MoneyFormat
import androidx.compose.foundation.layout.navigationBarsPadding
import com.truckerload.widget.WidgetDeepLink
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

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
        .collectAsState(initial = null)
    val userProfile by LocalUserProfileStore.current.profile.collectAsState()
    val welcomeName = remember(socialProfile, userProfile) {
        socialProfile?.displayName
            ?.takeIf { it.isNotBlank() && it !in setOf("Водитель", "Driver", "User") }
            ?: userProfile?.displayName
                ?.takeIf { it.isNotBlank() && it != userProfile?.email }
            ?: ""
    }
    val context = LocalContext.current
    val openDrawer = LocalOpenDrawer.current
    val isBotConfigured = remember(context) { TelegramTokenStore(context).hasToken() }
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(loadRepository, isBotConfigured, context))
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredResult by viewModel.filteredLoadsAndTotals.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val connectivity by remember(context) {
        ConnectivityObserver.observe(context)
    }.collectAsState(initial = ConnectivityStatus.Online)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshBotStatus()
        }
    }
    val filteredLoads = filteredResult.loads
    val totals = filteredResult.totals
    val datesWithLoads = filteredResult.datesWithLoads
    val weekLabel = remember(uiState.filter, uiState.selectedWeekLabel) {
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
        uiState.selectedDateLabel,
        uiState.selectedWeekLabel,
        totals,
    ) {
        viewModel.periodSummaryHeader(totals)
    }
    val rpmStore = LocalRpmThresholdsStore.current
    val rpmThresholds by rpmStore.thresholds.collectAsState()
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
                    Text(
                        stringResource(R.string.home_calendar_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = tc.TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = {
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
                        }) {
                            Text(stringResource(R.string.home_calendar_select_week), color = tc.AccentPrimary)
                        }
                        TextButton(onClick = { showCalendar = false }) {
                            Text(stringResource(R.string.common_close), color = tc.AccentPrimary)
                        }
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
                        DarkGlassScreenTitle(stringResource(R.string.home_brand_title))
                        if (welcomeName.isNotBlank()) {
                            Text(
                                stringResource(R.string.home_welcome, welcomeName),
                                style = AppTypography.Subtitle,
                                color = tc.AccentPrimary,
                            )
                        } else {
                            Text(
                                stringResource(R.string.app_tagline),
                                style = AppTypography.Subtitle,
                            )
                        }
                        weekLabel.takeIf { it.isNotBlank() && uiState.filter != LoadFilter.THIS_WEEK }?.let { week ->
                            Text(week, style = AppTypography.Subtitle)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.common_menu), tint = tc.TextPrimary)
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
                                TelegramSyncWorker.enqueueEnsureService(context.applicationContext, replace = true)
                            },
                            modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = stringResource(R.string.home_cd_sync_telegram))
                        }
                    }
                    IconButton(
                        onClick = { viewModel.setSearchExpanded(!uiState.isSearchExpanded) },
                        modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.home_cd_search))
                    }
                    BotStatusBadge(active = uiState.botStatusActive)
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
                        CircularProgressIndicator(color = tc.AccentPrimary)
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
    val pendingDeleteId by viewModel.pendingDeleteConfirmId.collectAsState()
    val swipeSettleGeneration by viewModel.swipeSettleGeneration.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    LaunchedEffect(deleteError) {
        val msg = deleteError ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        viewModel.clearDeleteError()
    }

    fun onRefresh() {
        refreshing = true
        TelegramSyncWorker.enqueueEnsureService(context.applicationContext, replace = true)
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.home_sync_triggered),
            android.widget.Toast.LENGTH_SHORT,
        ).show()
        scope.launch {
            delay(2500)
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
                item(key = "period_summary") {
                    PeriodSummarySection(header = summary)
                }
            }

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
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (listItems.isNotEmpty()) {
                item(key = "recent_header") {
                    DarkGlassSectionTitle(
                        text = stringResource(R.string.home_recent_loads),
                        emoji = "📋",
                        modifier = Modifier.padding(horizontal = adaptiveHorizontalPadding()),
                    )
                }
            }

            if (listItems.isEmpty()) {
                item(key = "empty") {
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

            item(key = "add_load_button") {
                TlButton(
                    onClick = onAddLoad,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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

@Composable
private fun PeriodSummarySection(header: HomeListItem.FilteredSectionHeader) {
    val tc = LocalTruckColors.current
    val totals = header.totals
    val gross = MoneyFormat.formatCurrency(totals.totalRate)
    val miles = "${MoneyFormat.formatNumber(totals.totalMiles)} mi"
    val rpm = stringResource(R.string.home_period_avg_rpm, totals.avgRpmFormatted)
    val summaryCd = stringResource(
        R.string.home_period_summary_cd,
        header.label,
        gross,
        miles,
        rpm,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = summaryCd },
    ) {
        Text(
            text = header.label.uppercase(),
            style = AppTypography.SectionTitle.copy(color = tc.TextPrimary),
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = gross,
                style = AppTypography.NumbersSmall.copy(
                    color = tc.AccentPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "•",
                style = AppTypography.Body.copy(color = tc.TextSecondary),
            )
            Text(
                text = miles,
                style = AppTypography.Body.copy(
                    color = tc.TextPrimary,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = "•",
                style = AppTypography.Body.copy(color = tc.TextSecondary),
            )
            Text(
                text = rpm,
                style = AppTypography.Body.copy(
                    color = SoftUiColors.PurpleEnd,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun YearSectionHeader(section: YearSection) {
    val tc = LocalTruckColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = adaptiveHorizontalPadding())) {
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
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, start = adaptiveHorizontalPadding())
    )
}
