package com.truckerload.presentation.screens.analytics

import com.truckerload.presentation.icons.AppIcons

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.truckerload.presentation.components.SoftActionChip
import com.truckerload.presentation.components.SoftAppPageScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.analytics.AnalyticsSummary
import com.truckerload.presentation.components.charts.DailyDistributionChart
import com.truckerload.presentation.components.charts.TopRoutesBarChart
import com.truckerload.presentation.components.charts.WeeklyRevenueLineChart
import com.truckerload.presentation.components.BentoGrid
import com.truckerload.presentation.components.BentoItem
import com.truckerload.presentation.components.LoadCard
import com.truckerload.presentation.components.RpmColorLegend
import com.truckerload.presentation.components.AnalyticsSkeleton
import com.truckerload.presentation.theme.focusAfterNavigate
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.utils.AnalyticsExportShare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit = {},
    onLoadClick: (String) -> Unit = {},
    onAbout: () -> Unit = {},
    onImprove: () -> Unit = {},
    embedded: Boolean = false,
) {
    val context = LocalContext.current
    val viewModel: AnalyticsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.shareReady) {
        val ready = uiState.shareReady ?: return@LaunchedEffect
        runCatching {
            AnalyticsExportShare.shareReport(context, ready.file, ready.format, ready.caption)
        }.onFailure {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.analytics_share_failed),
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
        viewModel.clearShareReady()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val tabletChrome = useNavigationRail()
    SoftAppPageScaffold(
        title = stringResource(R.string.analytics_title),
        subtitle = stringResource(R.string.analytics_subtitle),
        showBack = !embedded && !tabletChrome,
        onBack = onBack,
        showPhoneMenu = false,
        actions = {
            if (!embedded) {
                SoftActionChip(
                    icon = AppIcons.Info,
                    contentDescription = stringResource(R.string.analytics_cd_about),
                    onClick = onAbout,
                )
                SoftActionChip(
                    icon = AppIcons.EditNote,
                    contentDescription = stringResource(R.string.analytics_cd_feedback),
                    onClick = onImprove,
                )
                SoftActionChip(
                    icon = AppIcons.Share,
                    contentDescription = stringResource(R.string.analytics_export_cd),
                    onClick = { showShareDialog = true },
                )
            }
        },
    ) { padding ->
        AnalyticsScreenBody(
            padding = if (embedded) PaddingValues(0.dp) else padding,
            uiState = uiState,
            viewModel = viewModel,
            onLoadClick = onLoadClick,
        )
    }

    if (showShareDialog) {
        AnalyticsShareDialog(
            givenName = uiState.ownerGivenName,
            familyName = uiState.ownerFamilyName,
            onDismiss = { showShareDialog = false },
            onPick = { format, given, family ->
                showShareDialog = false
                viewModel.shareAnalytics(format, given, family)
            },
        )
    }
}

@Composable
private fun AnalyticsScreenBody(
    padding: PaddingValues,
    uiState: AnalyticsUiState,
    viewModel: AnalyticsViewModel,
    onLoadClick: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    BentoGlassScreenBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.isLoading) {
                AnalyticsSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusAfterNavigate(key = "analytics", enabled = !uiState.isLoading)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnalyticsPeriodPicker(
                        filter = uiState.filter,
                        onSelectPreset = viewModel::setPeriod,
                        onSelectYear = viewModel::selectYear,
                        onSelectMonth = viewModel::selectMonth,
                        onSelectWeek = viewModel::selectCalendarWeek,
                    )

                    uiState.summary?.let { summary ->
                        SummaryMetricsGrid(summary = summary)
                                                Text(
                            stringResource(R.string.analytics_rpm_legend_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                        RpmColorLegend(
                            compact = true,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    AnalyticsFinanceSection(finance = uiState.finance)

                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.analytics_weekly_revenue),
                                style = MaterialTheme.typography.titleMedium,
                                color = tc.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.analytics_weekly_revenue_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 4.dp),
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
                                    itemsIndexed(
                                        uiState.weeks,
                                        key = { _, week -> week.label },
                                    ) { index, week ->
                                        FilterChip(
                                            selected = uiState.selectedWeekIndex == index,
                                            onClick = { viewModel.selectWeek(index) },
                                            label = { Text(week.label) },
                                            colors = AppFilterChipDefaults.colors(),
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
                                            onClick = { onLoadClick(load.id) },
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
                            Text(
                                stringResource(R.string.analytics_top_routes_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 4.dp),
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
                            Text(
                                stringResource(R.string.analytics_daily_distribution_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 4.dp),
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

@Composable
private fun SummaryMetricsGrid(summary: AnalyticsSummary) {
    val tc = LocalTruckColors.current
    BentoGrid(
        items = listOf(
            BentoItem(
                value = summary.totalLoads.toString(),
                label = stringResource(R.string.analytics_total_loads),
                color = tc.AccentPrimary,
            ),
            BentoItem(
                value = MoneyFormat.formatCurrency(summary.totalGross),
                label = stringResource(R.string.analytics_total_gross),
                color = FinanceCockpitColors.SalaryAccent,
                highlight = true,
            ),
            BentoItem(
                value = MoneyFormat.formatNumber(summary.totalMiles),
                label = stringResource(R.string.analytics_total_miles),
                color = tc.AccentInfo,
            ),
            BentoItem(
                value = MoneyFormat.formatRpm(summary.avgRpm),
                label = stringResource(R.string.analytics_avg_rpm),
                color = BentoGlassTheme.GoalGradientEnd,
            ),
            BentoItem(
                value = MoneyFormat.formatCurrency(summary.avgGrossPerLoad),
                label = stringResource(R.string.analytics_avg_per_load),
                color = tc.AccentWarning,
            ),
            BentoItem(
                value = summary.bestWeek?.let { MoneyFormat.formatCurrency(it.gross) } ?: "—",
                label = stringResource(R.string.analytics_best_week),
                color = FinanceCockpitColors.NetProfitStart,
            ),
        ),
    )
}

