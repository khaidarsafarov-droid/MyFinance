package com.truckerload.presentation.screens.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SmartToy
import com.truckerload.presentation.components.SoftCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.SoftUiDimens
import com.truckerload.presentation.theme.DarkGlassScreenTitle
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSelectedStateStore
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalStatsSelectionStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.di.LocalWeekRepository
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.math.abs


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
    val welcomeName = remember(socialProfile, userProfile) {
        socialProfile?.displayName
            ?.takeIf { it.isNotBlank() && it !in setOf("Водитель", "Driver", "User") }
            ?: userProfile?.displayName
                ?.takeIf { it.isNotBlank() && it != userProfile?.email }
            ?: ""
    }
    val viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(
            LocalWeekRepository.current,
            LocalLoadRepository.current,
            LocalSelectedStateStore.current,
            LocalStatsSelectionStore.current
        )
    )
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
            val tc = LocalTruckColors.current
            TopAppBar(
                title = {
                    Column {
                        DarkGlassScreenTitle(stringResource(R.string.stats_title))
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

@Composable
private fun StatsAiOverlay(
    insight: String,
    actions: List<String>,
    onDismiss: () -> Unit,
    onOpenAdvisor: () -> Unit,
    onAction: (String) -> Unit
) {
    val tc = LocalTruckColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss)
    ) {
        SoftCard(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .fillMaxWidth(0.92f),
            onClick = {},
            contentPadding = 20.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.stats_ai_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.AccentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                    }
                }
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextPrimary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.take(3).forEach { action ->
                        FilterChip(
                            selected = false,
                            onClick = { onAction(action) },
                            label = { Text(action) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = tc.AccentPrimary.copy(alpha = 0.12f),
                                labelColor = tc.AccentSecondary
                            )
                        )
                    }
                }
                TextButton(onClick = onOpenAdvisor) {
                    Text(stringResource(R.string.stats_open_advisor))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}

@Composable
private fun ContextCard(month: Int, year: Int) {
    val tc = LocalTruckColors.current
    SoftCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = tc.TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.stats_context_month_year, monthShortLabel(month), year),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
        }
    }
}

@Composable
private fun HeroNetProfitCard(netProfit: Double, change: Double?, sparkline: List<Float>, onClick: () -> Unit) {
    val tc = LocalTruckColors.current
    SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.stats_label_net_profit),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
            Text("$${formatMoney(netProfit)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TrendText(change = change, goodWhenUp = false)
            Sparkline(values = sparkline, color = tc.AccentPrimary)
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    change: Double?,
    goodWhenUp: Boolean,
    sparkline: List<Float>?,
    onClick: (() -> Unit)? = null
) {
    val tc = LocalTruckColors.current
    SoftCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = tc.TextLabel
                    )
                }
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = tc.TextPrimary)
            TrendText(change = change, goodWhenUp = goodWhenUp)
            sparkline?.let {
                val sparkColor = when {
                    change == null -> tc.TextSecondary
                    goodWhenUp && change >= 0 -> tc.AccentProfit
                    goodWhenUp && change < 0 -> tc.AccentExpense
                    !goodWhenUp && change > 0 -> tc.AccentExpense
                    else -> tc.AccentProfit
                }
                Sparkline(it, sparkColor)
            }
        }
    }
}

@Composable
private fun RpmCard(modifier: Modifier = Modifier, rpm: Double, target: Double) {
    val tc = LocalTruckColors.current
    val progress = (rpm / target).toFloat().coerceIn(0f, 1f)
    SoftCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.stats_label_avg_rpm), color = tc.TextSecondary)
            Text("$${"%.2f".format(rpm)}/mi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.stats_rpm_goal, target),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(tc.ProgressTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(Brush.horizontalGradient(listOf(tc.AccentPrimary, tc.AccentSecondary)))
                )
            }
            Text(
                stringResource(R.string.stats_rpm_low_warning),
                style = MaterialTheme.typography.labelSmall,
                color = tc.AccentWarning
            )
        }
    }
}


@Composable
private fun Sparkline(values: List<Float>, color: Color) {
    if (values.isEmpty()) return
    val max = values.maxOrNull() ?: 1f
    val min = values.minOrNull() ?: 0f
    val range = (max - min).takeIf { it > 0f } ?: 1f
    Canvas(modifier = Modifier.fillMaxWidth().height(30.dp)) {
        val path = Path()
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - ((v - min) / range) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun TrendText(change: Double?, goodWhenUp: Boolean) {
    if (change == null) return
    val tc = LocalTruckColors.current
    val up = change >= 0
    val positiveOutcome = if (goodWhenUp) up else !up
    val color = if (positiveOutcome) tc.AccentProfit else tc.AccentExpense
    val arrow = if (up) "▲" else "▼"
    Text(
        text = stringResource(
            R.string.stats_trend_vs_prev_week,
            arrow,
            formatPct(change),
            stringResource(R.string.stats_vs_prev_week)
        ),
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
private fun SectionTitle(text: String) {
    val tc = LocalTruckColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = tc.TextSecondary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AdvancedStats(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val tc = LocalTruckColors.current
    Column(modifier = modifier.background(tc.Background), content = content)
}

@Composable
private fun SmartHeader(period: StatsPeriod, userName: String = "") {
    val tc = LocalTruckColors.current
    val periodLabel = when (period) {
        StatsPeriod.WEEK -> stringResource(R.string.common_week)
        StatsPeriod.MONTH -> stringResource(R.string.common_month)
        StatsPeriod.YEAR -> stringResource(R.string.common_year)
    }
    SoftCard {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (userName.isNotBlank()) {
                    stringResource(R.string.stats_header_greeting_named, userName)
                } else {
                    stringResource(R.string.stats_header_greeting)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tc.TextPrimary
            )
            Text(
                stringResource(R.string.stats_header_period_format, periodLabel),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary
            )
        }
    }
}

@Composable
private fun CerebrasInsightCard(
    visible: Boolean,
    insight: String,
    actions: List<String>,
    onActionClick: (String) -> Unit
) {
    val tc = LocalTruckColors.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320))
    ) {
        SoftCard(
            modifier = Modifier.border(1.dp, tc.GlassBorder, RoundedCornerShape(SoftUiDimens.CardRadius)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.stats_ai_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.AccentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextPrimary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.take(3).forEach { action ->
                        FilterChip(
                            selected = false,
                            onClick = { onActionClick(action) },
                            label = { Text(action) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = tc.AccentPrimary.copy(alpha = 0.12f),
                                labelColor = tc.AccentSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMagicBlock() {
    val tc = LocalTruckColors.current
    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(R.string.stats_empty_magic_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.stats_empty_magic_forecast),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MapPreviewCard(onOpenMap: () -> Unit) {
    val tc = LocalTruckColors.current
    SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenMap) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = stringResource(R.string.stats_map_open),
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.stats_map_open),
                    style = MaterialTheme.typography.titleMedium,
                    color = tc.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tc.TextLabel
            )
        }
    }
}

private fun percentChange(current: Double, previous: Double?): Double? {
    val prev = previous ?: return null
    if (abs(prev) < 0.0001) return null
    return ((current - prev) / prev) * 100.0
}

private fun formatPct(value: Double): String = "${if (value > 0) "+" else ""}${"%.1f".format(value)}%"

internal fun formatMoney(value: Double): String {
    return "%,.0f".format(value)
}

internal fun monthShortLabel(month: Int): String {
    val locale = Locale.getDefault()
    val short = DateFormatSymbols(locale).shortMonths.getOrNull((month - 1).coerceIn(0, 11)).orEmpty()
    return short.replace(".", "").replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
