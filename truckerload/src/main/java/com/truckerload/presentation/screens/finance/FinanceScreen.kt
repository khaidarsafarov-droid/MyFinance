package com.truckerload.presentation.screens.finance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.FinanceEmptyState
import com.truckerload.presentation.components.FinanceGlassCard
import com.truckerload.presentation.components.FinanceNetProfitCard
import com.truckerload.presentation.components.FinanceWeekStrip
import com.truckerload.presentation.components.ForecastCard
import com.truckerload.presentation.components.FuelAnalyticsCard
import com.truckerload.presentation.components.WeekCalendarPicker
import com.truckerload.presentation.theme.FinanceCockpitColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.compose.material.ExperimentalMaterialApi
@Composable
fun FinanceScreen(
    onAddPaycheck: () -> Unit,
    onAddDiesel: () -> Unit,
    onLoadClick: (String) -> Unit
) {
    val weekRepository = com.truckerload.presentation.di.LocalWeekRepository.current
    val loadRepository = com.truckerload.presentation.di.LocalLoadRepository.current
    val paycheckRepository = com.truckerload.presentation.di.LocalPaycheckRepository.current
    val dieselRepository = com.truckerload.presentation.di.LocalDieselRepository.current
    val viewModel: FinanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = FinanceViewModel.Factory(weekRepository, loadRepository, paycheckRepository, dieselRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.weekSummary
    var refreshing by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showQuickEntryMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val fabGlowTransition = rememberInfiniteTransition(label = "fab_glow")
    val fabGlowScale by fabGlowTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_glow_scale"
    )
    val fabGlowAlpha by fabGlowTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_glow_alpha"
    )
    val fabIconRotation by animateFloatAsState(
        targetValue = if (showQuickEntryMenu) 45f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 220),
        label = "fab_icon_rotation"
    )

    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        viewModel.refresh()
        scope.launch {
            delay(1500)
            refreshing = false
        }
    })

    Scaffold(
        containerColor = FinanceCockpitColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            FinanceCockpitColors.Background
                        )
                    )
                )
                .pullRefresh(pullRefreshState)
        ) {
            // Soft luxury glow layers for bright premium depth.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 64.dp)
                    .size(180.dp)
                    .blur(56.dp)
                    .background(FinanceCockpitColors.GlowIndigo, RoundedCornerShape(120.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 24.dp, top = 120.dp)
                    .size(150.dp)
                    .blur(52.dp)
                    .background(FinanceCockpitColors.GlowEmerald, RoundedCornerShape(120.dp))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                FinanceGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.period == FinancePeriod.WEEK,
                            onClick = { viewModel.setPeriod(FinancePeriod.WEEK) },
                            label = { Text(stringResource(R.string.common_week), color = FinanceCockpitColors.TextPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FinanceCockpitColors.ActiveDateBackground,
                                selectedLabelColor = FinanceCockpitColors.ActiveHighlight,
                                containerColor = Color.White.copy(alpha = 0.9f),
                                labelColor = FinanceCockpitColors.TextSecondary
                            )
                        )
                        FilterChip(
                            selected = uiState.period == FinancePeriod.MONTH,
                            onClick = { viewModel.setPeriod(FinancePeriod.MONTH) },
                            label = { Text(stringResource(R.string.common_month), color = FinanceCockpitColors.TextPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FinanceCockpitColors.ActiveDateBackground,
                                selectedLabelColor = FinanceCockpitColors.ActiveHighlight,
                                containerColor = Color.White.copy(alpha = 0.9f),
                                labelColor = FinanceCockpitColors.TextSecondary
                            )
                        )
                        FilterChip(
                            selected = uiState.period == FinancePeriod.YEAR,
                            onClick = { viewModel.setPeriod(FinancePeriod.YEAR) },
                            label = { Text(stringResource(R.string.common_year), color = FinanceCockpitColors.TextPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FinanceCockpitColors.ActiveDateBackground,
                                selectedLabelColor = FinanceCockpitColors.ActiveHighlight,
                                containerColor = Color.White.copy(alpha = 0.9f),
                                labelColor = FinanceCockpitColors.TextSecondary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.period == FinancePeriod.WEEK) {
                    AnimatedVisibility(
                        visible = showMonthPicker,
                        enter = slideInVertically(),
                        exit = slideOutVertically()
                    ) {
                        FinanceGlassCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            WeekCalendarPicker(
                                selectedMonth = uiState.calendarMonth,
                                selectedYear = uiState.calendarYear,
                                weeksInMonth = uiState.weeksInMonth,
                                selectedWeekNumber = uiState.weekNumber,
                                selectedWeekYear = uiState.year,
                                onMonthYearChange = { m, y -> viewModel.setMonthYear(m, y); showMonthPicker = false },
                                onWeekSelect = { w, y -> viewModel.selectWeek(w, y); showMonthPicker = false }
                            )
                        }
                    }
                    FinanceWeekStrip(
                        selectedMonth = uiState.calendarMonth,
                        selectedYear = uiState.calendarYear,
                        weeksInMonth = uiState.weeksInMonth,
                        selectedWeekNumber = uiState.weekNumber,
                        selectedWeekYear = uiState.year,
                        onMonthYearClick = { showMonthPicker = !showMonthPicker },
                        onWeekSelect = { w, y -> viewModel.selectWeek(w, y) },
                        onPreviousWeek = { viewModel.previousWeek() },
                        onNextWeek = { viewModel.nextWeek() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.weekForecast?.let { forecast ->
                        ForecastCard(
                            forecast = forecast,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AnimatedContent(
                    targetState = Triple(uiState.period, uiState.weekNumber, uiState.year),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "finance_period_transition"
                ) {
                    summary?.let { s ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            FinanceNetProfitCard(
                                amount = s.netProfit,
                                loadsCount = s.loadsCount,
                                totalMiles = s.totalMiles,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FinanceGlassCard(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Payments,
                                                contentDescription = null,
                                                tint = FinanceCockpitColors.SalaryAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                stringResource(R.string.finance_label_salary),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = FinanceCockpitColors.TextSecondary,
                                                modifier = Modifier.padding(start = 6.dp)
                                            )
                                        }
                                        Text(
                                            "$${String.format("%,.2f", s.paycheckAmount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = FinanceCockpitColors.SalaryAccent
                                        )
                                    }
                                }
                                FinanceGlassCard(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.LocalGasStation,
                                                contentDescription = null,
                                                tint = FinanceCockpitColors.DieselAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                stringResource(R.string.finance_label_diesel),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = FinanceCockpitColors.TextSecondary,
                                                modifier = Modifier.padding(start = 6.dp)
                                            )
                                        }
                                        Text(
                                            "-$${String.format("%,.2f", s.dieselAmount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = FinanceCockpitColors.DieselAccent
                                        )
                                    }
                                }
                            }
                            FinanceGlassCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                Column {
                                    Text(
                                        stringResource(R.string.finance_label_gross),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FinanceCockpitColors.TextSecondary
                                    )
                                    Text(
                                        "$${String.format("%,.2f", s.totalLoadRate)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = FinanceCockpitColors.TextPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                            stringResource(R.string.finance_label_salary),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FinanceCockpitColors.TextPrimary
                                )
                                IconButton(
                                    onClick = onAddPaycheck,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.common_add), tint = FinanceCockpitColors.SalaryAccent)
                                }
                            }
                            if (uiState.paycheck != null) {
                                FinanceGlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column {
                                        Text(
                                            uiState.paycheck!!.weekLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = FinanceCockpitColors.TextSecondary
                                        )
                                        Text(
                                            stringResource(R.string.finance_label_payout, uiState.paycheck!!.netAmount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = FinanceCockpitColors.SalaryAccent
                                        )
                                    }
                                }
                            } else {
                                FinanceGlassCard(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    FinanceEmptyState(
                                        message = stringResource(R.string.finance_empty_period),
                                        onAddClick = onAddPaycheck
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.finance_label_diesel),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FinanceCockpitColors.TextPrimary
                                )
                                IconButton(
                                    onClick = onAddDiesel,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.common_add), tint = FinanceCockpitColors.DieselAccent)
                                }
                            }
                            if (uiState.dieselList.isNotEmpty()) {
                                uiState.dieselList.forEach { d ->
                                    FinanceGlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column {
                                            Text(
                                                d.location ?: d.weekLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = FinanceCockpitColors.TextPrimary
                                            )
                                            Text(
                                                stringResource(R.string.finance_label_spent, d.totalAmount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = FinanceCockpitColors.DieselAccent
                                            )
                                        }
                                    }
                                }
                            } else {
                                FinanceGlassCard(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    FinanceEmptyState(
                                        message = stringResource(R.string.finance_empty_period),
                                        onAddClick = onAddDiesel
                                    )
                                }
                            }
                            uiState.fuelAnalytics?.let { fuel ->
                                FuelAnalyticsCard(
                                    analytics = fuel,
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showQuickEntryMenu,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { showQuickEntryMenu = false }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = showQuickEntryMenu,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpeedDialAction(
                            label = stringResource(R.string.finance_speed_dial_add_diesel),
                            icon = {
                                Icon(
                                    Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = FinanceCockpitColors.DieselAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showQuickEntryMenu = false
                                onAddDiesel()
                            }
                        )
                        SpeedDialAction(
                            label = stringResource(R.string.finance_speed_dial_add_salary),
                            icon = {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = FinanceCockpitColors.SalaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showQuickEntryMenu = false
                                onAddPaycheck()
                            }
                        )
                    }
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size((64 * fabGlowScale).dp)
                            .alpha(fabGlowAlpha)
                            .blur(14.dp)
                            .background(
                                FinanceCockpitColors.ActiveDateBackground,
                                RoundedCornerShape(40.dp)
                            )
                    )
                    FloatingActionButton(
                        onClick = { showQuickEntryMenu = !showQuickEntryMenu },
                        containerColor = FinanceCockpitColors.ActiveDateBackground,
                        contentColor = Color.White,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.finance_cd_quick_add),
                            modifier = Modifier.graphicsLayer { rotationZ = fabIconRotation }
                        )
                    }
                }
            }
            PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun SpeedDialAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FinanceCockpitColors.GlassCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, FinanceCockpitColors.GlassBorder),
        modifier = Modifier
            .sizeIn(minWidth = 220.dp, minHeight = 56.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = FinanceCockpitColors.TextPrimary
            )
        }
    }
}
