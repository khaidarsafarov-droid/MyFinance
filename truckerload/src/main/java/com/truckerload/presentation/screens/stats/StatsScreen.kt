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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSelectedStateStore
import com.truckerload.presentation.di.LocalStatsSelectionStore
import com.truckerload.presentation.di.LocalWeekRepository
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private data class LinePoint(
    val label: String,
    val revenue: Float,
    val expense: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onFinancialAdvisor: () -> Unit = {},
    onDieselDetail: () -> Unit = {},
    onNetProfitDetail: () -> Unit = {},
    onOpenMap: () -> Unit = {}
) {
    val aiRepository = LocalAiRepository.current
    val viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(
            LocalWeekRepository.current,
            LocalLoadRepository.current,
            LocalSelectedStateStore.current,
            LocalStatsSelectionStore.current
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resetFiltersDoneText = stringResource(R.string.stats_filters_reset_done)
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
        containerColor = Color(0xFFF5F7FA),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.stats_title), color = Color(0xFF1A1F36))
                        Text(
                            text = stringResource(R.string.stats_subtitle_rpm, uiState.avgRpm),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF667085)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAiOverlay = true }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.SmartToy, contentDescription = stringResource(R.string.stats_cd_advisor))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F7FA))
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        AdvancedStats(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            SmartHeader(period = uiState.statsPeriod)
                ContextCard(month = uiState.calendarMonth, year = uiState.calendarYear)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
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
                        containerColor = Color.White,
                        labelColor = Color(0xFF475467)
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
                sparkline = chartPoints.map { it.revenue - it.expense }
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
                    sparkline = chartPoints.map { it.revenue * 0.7f }
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
                onAction = { action ->
                    when {
                        "KY" in action -> { viewModel.setSelectedState("KY"); onOpenMap(); showAiOverlay = false }
                        "AZ" in action -> { viewModel.setSelectedState("AZ"); onOpenMap(); showAiOverlay = false }
                        "FL" in action -> { viewModel.setSelectedState("FL"); onOpenMap(); showAiOverlay = false }
                        aiKeywordDiesel in action.lowercase() -> { onDieselDetail(); showAiOverlay = false }
                        aiKeywordProfit in action.lowercase() -> { onNetProfitDetail(); showAiOverlay = false }
                        else -> {}
                    }
                }
            )
        }
        }
    }
}

@Composable
private fun StatsAiOverlay(
    insight: String,
    actions: List<String>,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss)
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .fillMaxWidth(0.92f)
                .clickable { },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        color = Color(0xFF1E3A8A),
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                    }
                }
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0F172A)
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
                                containerColor = Color(0xFFEFF6FF),
                                labelColor = Color(0xFF1D4ED8)
                            )
                        )
                    }
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
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF667085))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.stats_context_month_year, monthShortLabel(month), year),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A1F36)
            )
        }
    }
}

@Composable
private fun RevenueChartCard(points: List<LinePoint>) {
    var selectedIndex by remember { mutableIntStateOf(3) }
    val yMax = max(points.maxOf { it.revenue }, 12000f)
    val breakEven = points.map { it.expense }.average().toFloat()
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                stringResource(R.string.stats_chart_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A1F36)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures { tap ->
                                val slot = size.width / (points.size - 1).coerceAtLeast(1)
                                selectedIndex = (tap.x / slot).roundToInt().coerceIn(0, points.lastIndex)
                            }
                        }
                ) {
                    val slot = size.width / (points.size - 1).coerceAtLeast(1)
                    val toY: (Float) -> Float = { v ->
                        val normalized = (v / yMax).coerceIn(0f, 1f)
                        size.height - (normalized * (size.height - 12.dp.toPx())) - 6.dp.toPx()
                    }
                    val revenuePath = Path()
                    val expensePath = Path()
                    val revenueAreaPath = Path()
                    points.forEachIndexed { i, p ->
                        val x = i * slot
                        val ry = toY(p.revenue)
                        val ey = toY(p.expense)
                        if (i == 0) {
                            revenuePath.moveTo(x, ry)
                            expensePath.moveTo(x, ey)
                            revenueAreaPath.moveTo(x, size.height)
                            revenueAreaPath.lineTo(x, ry)
                        } else {
                            revenuePath.lineTo(x, ry)
                            expensePath.lineTo(x, ey)
                            revenueAreaPath.lineTo(x, ry)
                        }
                    }
                    revenueAreaPath.lineTo((points.lastIndex * slot), size.height)
                    revenueAreaPath.close()
                    drawPath(
                        path = revenueAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x552563EB), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = revenuePath,
                        color = Color(0xFF1E3A8A),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = expensePath,
                        color = Color(0xFFF59E0B),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    val breakY = toY(breakEven)
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(0f, breakY),
                        end = Offset(size.width, breakY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                    points.forEachIndexed { i, p ->
                        val x = i * slot
                        val selected = i == selectedIndex
                        drawCircle(
                            color = if (selected) Color(0xFF4F46E5) else Color(0xFF1E3A8A),
                            radius = if (selected) 5.dp.toPx() else 3.dp.toPx(),
                            center = Offset(x, toY(p.revenue))
                        )
                        drawCircle(
                            color = if (selected) Color(0xFFF97316) else Color(0xFFF59E0B),
                            radius = if (selected) 5.dp.toPx() else 3.dp.toPx(),
                            center = Offset(x, toY(p.expense))
                        )
                    }
                }
                val tip = points[selectedIndex]
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(8, 6) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            stringResource(R.string.stats_chart_peak_profit, tip.label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.stats_chart_revenue_value, formatMoney(tip.revenue.toDouble())),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            stringResource(R.string.stats_chart_expense_value, formatMoney(tip.expense.toDouble())),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            stringResource(R.string.stats_chart_diesel_value, "950"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { Text(it.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B)) }
            }
        }
    }
}

@Composable
private fun HeroNetProfitCard(netProfit: Double, change: Double?, sparkline: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.stats_label_net_profit),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1A1F36)
            )
            Text("$${formatMoney(netProfit)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TrendText(change = change, goodWhenUp = false)
            Sparkline(values = sparkline, color = Color(0xFF4F46E5))
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
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475467))
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1A1F36))
            TrendText(change = change, goodWhenUp = goodWhenUp)
            sparkline?.let {
                val sparkColor = when {
                    change == null -> Color(0xFF64748B)
                    goodWhenUp && change >= 0 -> Color(0xFF16A34A)
                    goodWhenUp && change < 0 -> Color(0xFFEF4444)
                    !goodWhenUp && change > 0 -> Color(0xFFEF4444)
                    else -> Color(0xFF16A34A)
                }
                Sparkline(it, sparkColor)
            }
        }
    }
}

@Composable
private fun RpmCard(modifier: Modifier = Modifier, rpm: Double, target: Double) {
    val progress = (rpm / target).toFloat().coerceIn(0f, 1f)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.stats_label_avg_rpm), color = Color(0xFF475467))
            Text("$${"%.2f".format(rpm)}/mi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.stats_rpm_goal, target),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF667085)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB))))
                )
            }
            Text(
                stringResource(R.string.stats_rpm_low_warning),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB45309)
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
    val up = change >= 0
    val positiveOutcome = if (goodWhenUp) up else !up
    val color = if (positiveOutcome) Color(0xFF16A34A) else Color(0xFFDC2626)
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
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFF475467),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AdvancedStats(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.background(Color(0xFFF5F7FA)), content = content)
}

@Composable
private fun SmartHeader(period: StatsPeriod) {
    val periodLabel = when (period) {
        StatsPeriod.WEEK -> stringResource(R.string.common_week)
        StatsPeriod.MONTH -> stringResource(R.string.common_month)
        StatsPeriod.YEAR -> stringResource(R.string.common_year)
    }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.stats_header_greeting),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1F36)
            )
            Text(
                stringResource(R.string.stats_header_period_format, periodLabel),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085)
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320))
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.86f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.stats_ai_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF1E3A8A),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0F172A)
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
                                containerColor = Color(0xFFEFF6FF),
                                labelColor = Color(0xFF1D4ED8)
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
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                color = Color(0xFF667085),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun buildIllustrativeChart(ui: StatsUiState): List<LinePoint> {
    val gross = ui.totalGross.takeIf { it > 0 } ?: 12817.0
    val diesel = ui.totalDiesel.takeIf { it > 0 } ?: 3412.0
    val revenueBase = (gross / 7.0).toFloat()
    val expenseBase = (diesel / 7.0).toFloat()
    val multipliers = when (ui.statsPeriod) {
        StatsPeriod.WEEK -> listOf(0.72f, 0.85f, 1.03f, 1.48f, 0.96f, 1.22f, 1.10f)
        StatsPeriod.MONTH -> listOf(0.90f, 1.00f, 1.08f, 1.28f, 1.12f, 1.18f, 1.14f)
        StatsPeriod.YEAR -> listOf(0.95f, 1.02f, 1.10f, 1.22f, 1.06f, 1.12f, 1.18f)
    }
    val monthLabel = monthShortLabel(ui.calendarMonth).lowercase(Locale.getDefault())
    return (1..7).mapIndexed { i, day ->
        LinePoint(
            label = "$day $monthLabel",
            revenue = (revenueBase * multipliers[i] * 4.1f),
            expense = (expenseBase * multipliers[(i + 1) % multipliers.size] * 2.1f)
        )
    }
}

@Composable
private fun MapPreviewCard(onOpenMap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenMap),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                    contentDescription = null,
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.stats_map_open),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1A1F36),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8)
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

private fun formatMoney(value: Double): String {
    return "%,.0f".format(value)
}

private fun monthShortLabel(month: Int): String {
    val locale = Locale.getDefault()
    val short = DateFormatSymbols(locale).shortMonths.getOrNull((month - 1).coerceIn(0, 11)).orEmpty()
    return short.replace(".", "").replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
