package com.truckerload.presentation.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.domain.model.analytics.AnalyticsSummary
import com.truckerload.presentation.components.charts.DailyDistributionChart
import com.truckerload.presentation.components.charts.TopRoutesBarChart
import com.truckerload.presentation.components.charts.WeeklyRevenueLineChart
import com.truckerload.presentation.components.LoadCard
import com.truckerload.presentation.components.RpmColorLegend
import com.truckerload.presentation.di.LocalAnalyticsRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    onAdvancedStats: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val repository = LocalAnalyticsRepository.current
    val viewModel: AnalyticsViewModel = viewModel(
        factory = AnalyticsViewModel.Factory(repository, context.applicationContext)
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.exportPath) {
        uiState.exportPath?.let { path ->
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.analytics_export_saved, path),
                android.widget.Toast.LENGTH_LONG
            ).show()
            viewModel.clearExportPath()
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.analytics_title),
                            color = tc.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.analytics_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = tc.TextSecondary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::exportAnalytics) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.analytics_export_cd),
                            tint = tc.AccentPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        BentoGlassScreenBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = tc.AccentPrimary,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PeriodFilterRow(
                            selected = uiState.period,
                            onSelect = viewModel::setPeriod,
                        )

                        uiState.summary?.let { summary ->
                            SummaryMetricsGrid(summary = summary)
                            RpmColorLegend(
                                compact = true,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.analytics_weekly_revenue),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tc.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                WeeklyRevenueLineChart(
                                    weeks = uiState.weeks,
                                    selectedIndex = uiState.selectedWeekIndex,
                                    onWeekSelected = viewModel::selectWeek,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                                if (uiState.weeks.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        itemsIndexed(uiState.weeks) { index, week ->
                                            FilterChip(
                                                selected = uiState.selectedWeekIndex == index,
                                                onClick = { viewModel.selectWeek(index) },
                                                label = { Text(week.label) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.2f),
                                                    selectedLabelColor = tc.AccentPrimary,
                                                ),
                                            )
                                        }
                                    }
                                }
                                val selectedWeek = uiState.selectedWeekIndex?.let { uiState.weeks.getOrNull(it) }
                                if (selectedWeek != null && uiState.selectedWeekLoads.isNotEmpty()) {
                                    Text(
                                        text = stringResource(
                                            R.string.analytics_week_loads_title,
                                            selectedWeek.label,
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = tc.TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 12.dp),
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        uiState.selectedWeekLoads.forEach { load ->
                                            LoadCard(
                                                load = load,
                                                onClick = {},
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.analytics_top_routes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tc.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                TopRoutesBarChart(
                                    routes = uiState.routes,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }

                        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.analytics_daily_distribution),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tc.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                DailyDistributionChart(
                                    dailyData = uiState.daily,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }

                        uiState.error?.let { err ->
                            Text(
                                text = err,
                                color = tc.AccentExpense,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodFilterRow(
    selected: AnalyticsPeriod,
    onSelect: (AnalyticsPeriod) -> Unit,
) {
    val tc = LocalTruckColors.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalyticsPeriod.entries.forEach { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = {
                    Text(
                        when (period) {
                            AnalyticsPeriod.LAST_12_WEEKS -> stringResource(R.string.analytics_period_12_weeks)
                            AnalyticsPeriod.LAST_6_MONTHS -> stringResource(R.string.analytics_period_6_months)
                            AnalyticsPeriod.ALL_TIME -> stringResource(R.string.analytics_period_all)
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tc.AccentPrimary.copy(alpha = 0.22f),
                    selectedLabelColor = tc.AccentPrimary,
                    containerColor = tc.CardBackground,
                    labelColor = tc.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun SummaryMetricsGrid(summary: AnalyticsSummary) {
    val tc = LocalTruckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_total_loads),
                value = summary.totalLoads.toString(),
                accent = tc.AccentPrimary,
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_total_gross),
                value = formatUsd(summary.totalGross),
                accent = FinanceCockpitColors.SalaryAccent,
                highlight = true,
            )
        }
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_total_miles),
                value = formatMiles(summary.totalMiles),
                accent = tc.AccentInfo,
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_avg_rpm),
                value = formatRpm(summary.avgRpm),
                accent = BentoGlassTheme.GoalGradientEnd,
            )
        }
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_avg_per_load),
                value = formatUsd(summary.avgGrossPerLoad),
                accent = tc.AccentWarning,
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.analytics_best_week),
                value = summary.bestWeek?.let { formatUsd(it.gross) } ?: "—",
                accent = FinanceCockpitColors.NetProfitStart,
            )
        }
    }
}

private fun formatUsd(value: Double): String =
    String.format(Locale.US, "$%,.0f", value)

private fun formatMiles(value: Double): String =
    String.format(Locale.US, "%,.0f", value)

private fun formatRpm(value: Double): String =
    String.format(Locale.US, "$%.2f", value)
