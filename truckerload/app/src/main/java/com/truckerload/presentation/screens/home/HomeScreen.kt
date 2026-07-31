package com.truckerload.presentation.screens.home

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.presentation.components.AuthStatusBanner
import com.truckerload.presentation.components.LocalOpenDrawer
import com.truckerload.presentation.connectivity.ConnectivityObserver
import com.truckerload.presentation.connectivity.ConnectivityStatus
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.widget.WidgetDeepLink

@OptIn(ExperimentalMaterial3Api::class)
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
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredResult by viewModel.filteredLoadsAndTotals.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val connectivity by remember(context) {
        ConnectivityObserver.observe(context)
    }.collectAsStateWithLifecycle(initialValue = ConnectivityStatus.Online)
    val filteredLoads = filteredResult.loads
    val totals = filteredResult.totals
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

    fun openCalendarDialog() {
        // Always open on the current month (or selected day), not a stale future month.
        val anchor = uiState.selectedDate
            ?.let { runCatching { java.time.LocalDate.parse(it.take(10)) }.getOrNull() }
            ?: java.time.LocalDate.now()
        calendarYear = anchor.year
        calendarMonth = anchor.monthValue
        showCalendar = true
    }

    if (showCalendar) {
        HomeCalendarDialog(
            viewModel = viewModel,
            calendarYear = calendarYear,
            calendarMonth = calendarMonth,
            selectedDate = uiState.selectedDate,
            onYearMonthChange = { y, m ->
                calendarYear = y
                calendarMonth = m
            },
            onDateSelect = { date ->
                viewModel.selectDate(date)
                showCalendar = false
            },
            onWeekSelect = { start, end, label ->
                viewModel.selectWeek(start, end, label)
                showCalendar = false
            },
            onDismiss = { showCalendar = false },
        )
    }

    LaunchedEffect(Unit) {
        if (WidgetDeepLink.consumeOpenJournalThisWeek(context)) {
            viewModel.setFilter(LoadFilter.THIS_WEEK)
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            HomeScreenTopBar(
                welcomeName = welcomeName,
                weekLabel = weekLabel,
                filter = uiState.filter,
                openDrawer = openDrawer,
                onSearchToggle = { viewModel.setSearchExpanded(!uiState.isSearchExpanded) },
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
                        viewModel = viewModel,
                        filteredLoads = filteredLoads,
                        useRoomPaging = useRoomPaging,
                        pagedLoads = pagedLoads,
                        onLoadClick = onLoadClick,
                        onAddLoad = onAddLoad,
                        context = context,
                        onOpenCalendar = { openCalendarDialog() },
                        onLoadCamera = onLoadCamera,
                        onLoadScan = onLoadScan,
                    )
                }
                if (isInitialLoading) {
                    HomeInitialLoadingOverlay()
                }
            }
        }
    )
}
