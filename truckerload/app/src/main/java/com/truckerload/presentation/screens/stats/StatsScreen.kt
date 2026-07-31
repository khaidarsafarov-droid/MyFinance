package com.truckerload.presentation.screens.stats

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.ForestScreenTitle
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    showBack: Boolean = true,
    onSettings: () -> Unit = {},
    onFinancialAdvisor: () -> Unit = {},
    onDieselDetail: () -> Unit = {},
    onNetProfitDetail: () -> Unit = {},
    onPaycheckDetail: () -> Unit = {},
    onOpenMap: () -> Unit = {}
) {
    val aiRepository = LocalAiRepository.current
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
    val viewModel: StatsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resetFiltersDoneText = stringResource(R.string.stats_filters_reset_done)
    val actionUnavailableText = stringResource(R.string.stats_action_unavailable)
    val aiKeywordDiesel = stringResource(R.string.stats_ai_keyword_diesel)
    val aiKeywordProfit = stringResource(R.string.stats_ai_keyword_profit)
    val defaultInsightActions = listOf(
        stringResource(R.string.stats_ai_action_routes_ky),
        stringResource(R.string.stats_ai_action_prices_az),
        stringResource(R.string.stats_ai_action_profit_fl)
    )
    val defaultInsightText = stringResource(R.string.stats_ai_default_insight)
    val anomalyFuelGrowthFormat = stringResource(R.string.stats_ai_anomaly_fuel_growth)
    val fallbackAnomalyText = stringResource(R.string.stats_ai_anomaly_fuel_growth_fallback)
    var insightText by remember { mutableStateOf(defaultInsightText) }
    var insightActions by remember { mutableStateOf(defaultInsightActions) }
    var showInsight by remember { mutableStateOf(false) }
    var showAiOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val chartPoints = remember(uiState.statsPeriod, uiState.totalGross, uiState.totalDiesel) {
        buildIllustrativeChart(uiState)
    }
    val topStatesForAi = remember(uiState.topStatesByRevenue) {
        uiState.topStatesByRevenue.take(3).map { it.state }
    }
    val anomalies = remember(uiState.totalDiesel, uiState.prevDiesel) {
        val dieselGrowth = percentChange(uiState.totalDiesel, uiState.prevDiesel)
        if (dieselGrowth != null) {
            String.format(Locale.getDefault(), anomalyFuelGrowthFormat, kotlin.math.abs(dieselGrowth))
        } else {
            fallbackAnomalyText
        }
    }
    LaunchedEffect(
        uiState.statsPeriod,
        uiState.avgRpm,
        uiState.netProfit,
        uiState.totalDiesel,
        uiState.totalMiles,
        topStatesForAi.joinToString(",")
    ) {
        aiRepository ?: return@LaunchedEffect
        val aiResult = aiRepository.generateRealTimeLogisticsInsight(
            userName = "driver",
            rpm = uiState.avgRpm,
            profit = uiState.netProfit,
            fuelCost = uiState.totalDiesel,
            miles = uiState.totalMiles,
            topStates = topStatesForAi,
            anomalies = anomalies
        )
        aiResult.onSuccess { insight ->
            insightText = insight.insight
            insightActions = insight.actions.take(3).ifEmpty { defaultInsightActions }
            showInsight = true
        }.onFailure {
            showInsight = true
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        ForestScreenTitle(stringResource(R.string.stats_title))
                        Text(
                            text = stringResource(R.string.stats_subtitle_rpm, uiState.avgRpm),
                            style = AppTypography.Subtitle,
                        )
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAiOverlay = true }, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(Icons.Default.SmartToy, contentDescription = stringResource(R.string.stats_cd_advisor))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BentoGlassTheme.ScreenBackground)
            )
        },
    ) { padding ->
        BentoGlassScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        AdvancedStats(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            SmartHeader(period = uiState.statsPeriod, userName = welcomeName)
                ContextCard(month = uiState.calendarMonth, year = uiState.calendarYear)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tc = LocalTruckColors.current
                listOf(StatsPeriod.WEEK, StatsPeriod.MONTH, StatsPeriod.YEAR).forEach { period ->
                    FilterChip(
                        selected = uiState.statsPeriod == period,
                        onClick = { viewModel.setStatsPeriod(period) },
                        label = {
                            Text(
                                when (period) {
                                    StatsPeriod.WEEK -> stringResource(R.string.common_week)
                                    StatsPeriod.MONTH -> stringResource(R.string.common_month)
                                    StatsPeriod.YEAR -> stringResource(R.string.common_year)
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = tc.AccentPrimary,
                            containerColor = tc.CardBackground,
                            labelColor = tc.TextSecondary
                        )
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = {
                        viewModel.resetFiltersToDefault()
                        scope.launch { snackbarHostState.showSnackbar(message = resetFiltersDoneText) }
                    },
                    label = { Text(stringResource(R.string.stats_reset_filters)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = tc.CardBackground,
                        labelColor = tc.TextSecondary
                    )
                )
            }

            RevenueChartCard(points = chartPoints)
            CerebrasInsightCard(
                visible = showInsight,
                insight = insightText,
                actions = insightActions
            ) { action ->
                when {
                    "KY" in action -> { viewModel.setSelectedState("KY"); onOpenMap() }
                    "AZ" in action -> { viewModel.setSelectedState("AZ"); onOpenMap() }
                    "FL" in action -> { viewModel.setSelectedState("FL"); onOpenMap() }
                    aiKeywordDiesel in action.lowercase() -> onDieselDetail()
                    aiKeywordProfit in action.lowercase() -> onNetProfitDetail()
                }
            }

            if (uiState.loadCount == 0) {
                EmptyMagicBlock()
                Spacer(modifier = Modifier.height(8.dp))
                return@Column
            }

            SectionTitle(stringResource(R.string.stats_section_key_metrics))
            HeroNetProfitCard(
                netProfit = uiState.netProfit,
                change = percentChange(uiState.netProfit, uiState.prevNetProfit),
                sparkline = chartPoints.map { it.revenue - it.expense },
                onClick = onNetProfitDetail,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stats_label_gross_income),
                    value = "$${formatMoney(uiState.totalGross)}",
                    change = percentChange(uiState.totalGross, uiState.prevGross),
                    goodWhenUp = true,
                    sparkline = chartPoints.map { it.revenue }
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stats_label_salary),
                    value = "$${formatMoney(uiState.totalPaycheck)}",
                    change = percentChange(uiState.totalPaycheck, uiState.prevPaycheck),
                    goodWhenUp = true,
                    sparkline = chartPoints.map { it.revenue * 0.7f },
                    onClick = onPaycheckDetail,
                )
            }

            SectionTitle(stringResource(R.string.stats_section_operational_efficiency))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RpmCard(
                    modifier = Modifier.weight(1f),
                    rpm = uiState.avgRpm,
                    target = 2.50
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stats_label_miles),
                    value = "${formatMoney(uiState.totalMiles)} mi",
                    change = percentChange(uiState.totalMiles, uiState.prevMiles),
                    goodWhenUp = true,
                    sparkline = chartPoints.map { it.revenue * 0.52f }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stats_label_trips),
                    value = "%.2f".format(uiState.loadCount.toDouble()),
                    change = percentChange(uiState.loadCount.toDouble(), uiState.prevLoadCount?.toDouble()),
                    goodWhenUp = true,
                    sparkline = null
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.finance_label_diesel),
                    value = "$${formatMoney(uiState.totalDiesel)}",
                    change = percentChange(uiState.totalDiesel, uiState.prevDiesel),
                    goodWhenUp = false,
                    sparkline = chartPoints.map { it.expense },
                    onClick = onDieselDetail
                )
            }

            SectionTitle(stringResource(R.string.stats_section_geo_efficiency))
            MapPreviewCard(onOpenMap = onOpenMap)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showAiOverlay) {
            StatsAiOverlay(
                insight = insightText,
                actions = insightActions,
                onDismiss = { showAiOverlay = false },
                onOpenAdvisor = {
                    showAiOverlay = false
                    onFinancialAdvisor()
                },
                onAction = { action ->
                    when {
                        "KY" in action -> { viewModel.setSelectedState("KY"); onOpenMap(); showAiOverlay = false }
                        "AZ" in action -> { viewModel.setSelectedState("AZ"); onOpenMap(); showAiOverlay = false }
                        "FL" in action -> { viewModel.setSelectedState("FL"); onOpenMap(); showAiOverlay = false }
                        aiKeywordDiesel in action.lowercase() -> { onDieselDetail(); showAiOverlay = false }
                        aiKeywordProfit in action.lowercase() -> { onNetProfitDetail(); showAiOverlay = false }
                        else -> {
                            scope.launch { snackbarHostState.showSnackbar(actionUnavailableText) }
                        }
                    }
                }
            )
        }
        }
        }
    }
}
