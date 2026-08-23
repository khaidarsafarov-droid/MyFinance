package com.truckerload.presentation.screens.home

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
                Box(modifier = Modifier.width(320.dp)) {
                    BentoGlassSearchField(
                        value = searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = stringResource(R.string.tablet_home_search_hint),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.LocalShipping,
                    tint = Color(0xFFD9ECF8),
                    label = stringResource(R.string.tablet_stat_loads),
                    value = totals.loadCount.toString(),
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.Route,
                    tint = Color(0xFFE4F2E8),
                    label = stringResource(R.string.tablet_stat_miles),
                    value = MoneyFormat.formatNumber(totals.totalMiles),
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.Speed,
                    tint = Color(0xFFF7F0D9),
                    label = stringResource(R.string.tablet_stat_rpm),
                    value = rpmLabel,
                    hero = true,
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.LocalGasStation,
                    tint = Color(0xFFE8E4F5),
                    label = stringResource(R.string.tablet_stat_gross),
                    value = gross,
                    hero = true,
                )
            }
        }

        item(key = "tablet_panels") {
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
