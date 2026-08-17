package com.truckerload.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.PeriodFilterStyle
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassSearchField
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes
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
                    icon = Icons.Outlined.LocalShipping,
                    tint = Color(0xFFD9ECF8),
                    label = stringResource(R.string.tablet_stat_loads),
                    value = totals.loadCount.toString(),
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Route,
                    tint = Color(0xFFE4F2E8),
                    label = stringResource(R.string.tablet_stat_miles),
                    value = MoneyFormat.formatNumber(totals.totalMiles),
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Speed,
                    tint = Color(0xFFF7F0D9),
                    label = stringResource(R.string.tablet_stat_rpm),
                    value = rpmLabel,
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.LocalGasStation,
                    tint = Color(0xFFE8E4F5),
                    label = stringResource(R.string.tablet_stat_gross),
                    value = gross,
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

        if (recentLoads.isEmpty() && uiState.filter != LoadFilter.ALL) {
            item(key = "tablet_empty_hint") {
                Text(
                    text = stringResource(R.string.home_empty_filtered_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SoftHeroCard(
    periodLabel: String,
    gross: String,
    subtitle: String,
    onAddLoad: () -> Unit,
    filterContent: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .clip(SoftUiShapes.Card)
            .background(SoftUiColors.ForestAccent),
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalShipping,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(140.dp),
        )
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = periodLabel.uppercase(),
                style = AppTypography.Caption.copy(color = SoftUiColors.Sage),
            )
            Text(
                text = gross,
                style = AppTypography.NumbersLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftUiColors.Sage,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAddLoad,
                        )
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_add_load_button),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SoftUiColors.ForestPrimary,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    filterContent()
                }
            }
        }
    }
}

@Composable
private fun SoftStatCard(
    modifier: Modifier,
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SoftUiColors.ForestPrimary)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = tc.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
    }
}

@Composable
private fun SoftRecentCard(
    modifier: Modifier,
    loads: List<Load>,
    onLoadClick: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp)
            .heightIn(min = 280.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.home_recent_loads),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tc.TextPrimary,
        )
        if (loads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_empty_this_week),
                    color = tc.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            loads.forEach { load ->
                SoftLoadRow(load = load, onClick = { onLoadClick(load.id) })
            }
        }
    }
}

@Composable
private fun SoftLoadRow(load: Load, onClick: () -> Unit) {
    val tc = LocalTruckColors.current
    val route = listOf(load.pointA, load.pointB)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" → ")
        .ifBlank { load.tripId.ifBlank { "—" } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoftUiColors.Sage.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = route,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tc.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${MoneyFormat.formatCurrency(load.totalRate)} · ${MoneyFormat.formatNumber(load.totalMiles)} mi",
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = load.date.take(10),
            style = MaterialTheme.typography.labelMedium,
            color = SoftUiColors.ForestMuted,
        )
    }
}

@Composable
private fun SoftGoalCard(
    modifier: Modifier,
    weeklyGoal: Double,
    currentGross: Double,
    progress: Float,
    onOpenWeeklyGoal: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenWeeklyGoal,
            )
            .padding(18.dp)
            .heightIn(min = 280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Flag, contentDescription = null, tint = SoftUiColors.ForestAccent)
            Text(
                text = stringResource(R.string.nav_weekly_goal),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tc.TextPrimary,
            )
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = SoftUiColors.ForestAccent,
                trackColor = SoftUiColors.Sage,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = tc.TextPrimary,
                )
            }
        }
        Text(
            text = MoneyFormat.formatCurrency(currentGross),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = tc.TextPrimary,
        )
        Text(
            text = if (weeklyGoal > 0) {
                stringResource(
                    R.string.tablet_home_goal_of,
                    MoneyFormat.formatCurrency(weeklyGoal),
                )
            } else {
                stringResource(R.string.tablet_home_goal_set)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = SoftUiColors.ForestAccent,
            trackColor = SoftUiColors.Sage,
            strokeCap = StrokeCap.Round,
        )
    }
}
