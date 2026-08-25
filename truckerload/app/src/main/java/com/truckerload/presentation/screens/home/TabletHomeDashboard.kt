package com.truckerload.presentation.screens.home

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.PeriodFilterStyle
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.theme.BentoGlassSearchField
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.useWideTabletSidebar
import java.util.Locale

/**
 * Soft-UI tablet journal dashboard (hero + stats + recent loads + weekly goal).
 * No Telegram promo banner — sync stays in Settings.
 */
@Composable
internal fun TabletHomeDashboard(
    paddingValues: PaddingValues,
    uiState: HomeUiState,
    searchQuery: String,
    periodSummary: HomeListItem.FilteredSectionHeader?,
    totals: LoadFilterUseCase.Totals,
    recentLoads: List<Load>,
    viewModel: HomeViewModel,
    onLoadClick: (String) -> Unit,
    onAddLoad: () -> Unit,
    onOpenWeeklyGoal: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenArchive: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val goalStore = LocalWeeklyProfitGoalStore.current
    val weeklyGoal by goalStore.goalAmount.collectAsStateWithLifecycle()
    val periodLabel = periodSummary?.label
        ?: stringResource(R.string.home_filter_this_week)
    val gross = MoneyFormat.formatCurrency(totals.totalRate)
    val milesLabel = "${MoneyFormat.formatNumber(totals.totalMiles)} mi"
    val rpmLabel = if (totals.totalMiles > 0) {
        String.format(Locale.US, "$%.2f", totals.avgRpm)
    } else {
        "—"
    }
    val wide = useWideTabletSidebar()
    val goalProgress = if (weeklyGoal > 0) {
        (totals.totalRate / weeklyGoal).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "tablet_header") {
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nav_logbook),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = tc.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Box(modifier = Modifier.widthIn(min = 200.dp, max = 360.dp)) {
                        BentoGlassSearchField(
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = stringResource(R.string.tablet_home_search_hint),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nav_logbook),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = tc.TextPrimary,
                    )
                    BentoGlassSearchField(
                        value = searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = stringResource(R.string.tablet_home_search_hint),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item(key = "tablet_hero") {
            SoftHeroCard(
                periodLabel = periodLabel,
                gross = gross,
                subtitle = stringResource(
                    R.string.tablet_home_hero_subtitle,
                    totals.loadCount,
                    milesLabel,
                    rpmLabel,
                ),
                onAddLoad = onAddLoad,
                filterContent = {
                    HomePeriodFilterDropdown(
                        currentFilter = uiState.filter,
                        selectedYear = uiState.selectedYear,
                        selectedDateLabel = uiState.selectedDateLabel,
                        selectedWeekLabel = uiState.selectedWeekLabel,
                        onFilterSelected = viewModel::setFilter,
                        onOpenCalendar = onOpenCalendar,
                        onOpenArchive = onOpenArchive,
                        style = PeriodFilterStyle.HeroPill,
                    )
                },
            )
        }

        item(key = "tablet_stats") {
            TabletStatsGrid(
                loadCount = totals.loadCount.toString(),
                miles = MoneyFormat.formatNumber(totals.totalMiles),
                rpm = rpmLabel,
                gross = gross,
                compact = !wide,
            )
        }

        item(key = "tablet_panels") {
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SoftRecentCard(
                        modifier = Modifier.weight(1.35f),
                        loads = recentLoads.take(6),
                        onLoadClick = onLoadClick,
                    )
                    SoftGoalCard(
                        modifier = Modifier.weight(1f),
                        weeklyGoal = weeklyGoal,
                        currentGross = totals.totalRate,
                        progress = goalProgress,
                        onOpenWeeklyGoal = onOpenWeeklyGoal,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SoftRecentCard(
                        modifier = Modifier.fillMaxWidth(),
                        loads = recentLoads.take(6),
                        onLoadClick = onLoadClick,
                    )
                    SoftGoalCard(
                        modifier = Modifier.fillMaxWidth(),
                        weeklyGoal = weeklyGoal,
                        currentGross = totals.totalRate,
                        progress = goalProgress,
                        onOpenWeeklyGoal = onOpenWeeklyGoal,
                    )
                }
            }
        }

        if (recentLoads.isEmpty()) {
            item(key = "tablet_empty_hint") {
                HomeEmptyJournal(
                    title = if (uiState.filter == LoadFilter.THIS_WEEK) {
                        stringResource(R.string.home_empty_this_week)
                    } else {
                        stringResource(R.string.home_empty_filtered_body)
                    },
                    body = stringResource(R.string.ux_home_empty_reciprocity),
                    ctaLabel = stringResource(R.string.home_empty_cta),
                    onCta = onAddLoad,
                )
            }
        }
    }
}
