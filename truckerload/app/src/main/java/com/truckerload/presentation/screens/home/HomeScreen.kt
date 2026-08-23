package com.truckerload.presentation.screens.home

import com.truckerload.presentation.icons.AppIcons

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.truckerload.presentation.components.SyncStatusBanner
import com.truckerload.presentation.components.LocalOpenDrawer
import com.truckerload.presentation.components.QuickActionsBottomSheet
import com.truckerload.presentation.connectivity.ConnectivityObserver
import com.truckerload.presentation.connectivity.ConnectivityStatus
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.di.LocalProfileRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.utils.FeedbackManager
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
    onCamera: () -> Unit = {},
    onScan: () -> Unit = {},
    onAddDiesel: () -> Unit = {},
    onVoiceAssistant: () -> Unit = {},
    onLoadCamera: (loadId: String, tripId: String, loadDate: String) -> Unit = { _, _, _ -> },
    onLoadScan: (loadId: String, tripId: String, loadDate: String) -> Unit = { _, _, _ -> },
    onOpenPrivacy: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val socialProfile by LocalProfileRepository.current.watchMyEnhancedProfile()
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
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val undoDeleteId by viewModel.undoDeleteLoadId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showQuickActions by remember { mutableStateOf(false) }
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

    if (showQuickActions) {
        QuickActionsBottomSheet(
            onDismiss = { showQuickActions = false },
            onAddLoad = onAddLoad,
            onCamera = onCamera,
            onScan = onScan,
            onAddDiesel = onAddDiesel,
            onVoiceAssistant = onVoiceAssistant,
        )
    }

    LaunchedEffect(Unit) {
        if (WidgetDeepLink.consumeOpenJournalThisWeek(context)) {
            viewModel.setFilter(LoadFilter.THIS_WEEK)
        }
    }

    val undoLabel = stringResource(R.string.common_undo)
    val deletedLabel = stringResource(R.string.home_load_deleted_snackbar)
    LaunchedEffect(undoDeleteId) {
        if (undoDeleteId == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedLabel,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDeleteLoad()
        }
        // Dismiss / timeout: HomeViewModel commits after UNDO_DELETE_WINDOW_MS.
    }

    val tabletChrome = com.truckerload.presentation.utils.useNavigationRail()

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            if (!tabletChrome) {
                HomeScreenTopBar(
                    welcomeName = welcomeName,
                    weekLabel = weekLabel,
                    filter = uiState.filter,
                    openDrawer = openDrawer,
                    onSearchToggle = { viewModel.setSearchExpanded(!uiState.isSearchExpanded) },
                    onOpenPrivacy = onOpenPrivacy,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = tc.CardBackground,
                    contentColor = tc.TextPrimary,
                    actionColor = tc.AccentPrimary,
                )
            }
        },
        floatingActionButton = {
            if (!tabletChrome) {
                FloatingActionButton(
                    onClick = {
                        FeedbackManager.onNavSelect()
                        showQuickActions = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(UiDimens.FabSize),
                ) {
                    Icon(
                        AppIcons.Add,
                        contentDescription = stringResource(R.string.quick_actions_title),
                    )
                }
            }
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
                                .background(tc.TextSecondary.copy(alpha = 0.16f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    AuthStatusBanner()
                    SyncStatusBanner()
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
                        context = context,
                        onOpenCalendar = { openCalendarDialog() },
                        onLoadCamera = onLoadCamera,
                        onLoadScan = onLoadScan,
                        onAddLoad = onAddLoad,
                        onOpenWeeklyGoal = onWeeklyGoal,
                        onAddDiesel = onAddDiesel,
                        onOpenProfile = onSettings,
                        periodTotals = totals,
                        onOpenPrivacy = onOpenPrivacy,
                    )
                }
                if (HomeRefreshPolicy.shouldShowInitialOverlay(isInitialLoading, isRefreshing)) {
                    HomeInitialLoadingOverlay()
                }
            }
        }
    )
}
