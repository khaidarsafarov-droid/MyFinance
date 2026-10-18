package com.truckerload.presentation.screens.home

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.presentation.components.computeRpm
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalSemantics
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.utils.MoneyFormat

@Composable
internal fun SoftHeroCard(
    periodLabel: String,
    gross: String,
    miles: String,
    rpm: String,
    filterContent: @Composable () -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onPrimaryMuted = onPrimary.copy(alpha = 0.88f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Icon(
            imageVector = AppIcons.LocalShipping,
            contentDescription = null,
            tint = onPrimary.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .size(112.dp),
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = periodLabel.uppercase(),
                style = AppTypography.CaptionMuted.copy(color = onPrimaryMuted),
            )
            HeroPeriodFigures(
                gross = gross,
                miles = miles,
                rpmValue = rpm,
                rpmLabel = stringResource(R.string.home_period_avg_rpm_label),
                onPrimary = onPrimary,
                onPrimaryMuted = onPrimaryMuted,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )
            Box(modifier = Modifier.padding(top = 8.dp)) {
                filterContent()
            }
        }
    }
}

@Composable
internal fun SoftAddLoadButton(onAddLoad: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = SoftUiElevation.Button,
                shape = SoftUiShapes.Button,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            )
            .clip(SoftUiShapes.Button)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAddLoad,
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_add_load_button),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
internal fun SoftStatCard(
    modifier: Modifier,
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    hero: Boolean = false,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
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
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = value,
            style = if (hero) AppTypography.HeroNumberCompact else AppTypography.NumbersMetric,
            color = tc.TextNumbers,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = AppTypography.CaptionMuted,
        )
    }
}

@Composable
internal fun SoftRecentCard(
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
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            )
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
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
                    text = stringResource(R.string.ux_home_empty_reciprocity),
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
internal fun SoftLoadRow(load: Load, onClick: () -> Unit) {
    val tc = LocalTruckColors.current
    val cs = MaterialTheme.colorScheme
    val rpm = computeRpm(load.totalRate, load.drivenMiles)
    val stops = load.stopCount.takeIf { it > 0 } ?: (load.puCount + load.delCount)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            )
            .clip(SoftUiShapes.Card)
            .background(cs.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = load.tripId.ifBlank { "—" },
                style = AppTypography.CardTitle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = tc.TextPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = load.date.take(10),
                style = AppTypography.CaptionMuted,
                maxLines = 1,
            )
        }
        Text(
            text = formatLoadRoute(load),
            style = AppTypography.CardRoute.copy(color = tc.TextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(
                R.string.load_card_summary_line,
                stops,
                MoneyFormat.formatNumber(load.drivenMiles),
                MoneyFormat.formatCurrency(load.totalRate, decimals = 2),
            ),
            style = AppTypography.CaptionMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (rpm != null) {
            Text(
                text = MoneyFormat.formatRpm(rpm),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(SoftUiShapes.Chip)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
internal fun SoftGoalCard(
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
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            )
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenWeeklyGoal,
            )
            .padding(18.dp)
            .heightIn(min = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(AppIcons.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.nav_weekly_goal),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tc.TextPrimary,
            )
        }
        if (weeklyGoal > 0) {
            Text(
                text = MoneyFormat.formatCurrency(currentGross),
                style = AppTypography.HeroNumberCompact,
                color = tc.TextNumbers,
            )
            Text(
                text = stringResource(
                    R.string.tablet_home_goal_of,
                    MoneyFormat.formatCurrency(weeklyGoal),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(SoftUiShapes.Chip)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.tablet_home_goal_set),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            Text(
                text = stringResource(R.string.ux_next_set_goal),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun TabletStatsGrid(
    loadCount: String,
    miles: String,
    rpm: String,
    gross: String,
    compact: Boolean,
) {
    @Composable
    fun StatsRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatsRow {
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.LocalShipping,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    label = stringResource(R.string.tablet_stat_loads),
                    value = loadCount,
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.Route,
                    tint = LocalSemantics.current.successContainer,
                    label = stringResource(R.string.tablet_stat_miles),
                    value = miles,
                )
            }
            StatsRow {
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.Speed,
                    tint = LocalSemantics.current.warningContainer,
                    label = stringResource(R.string.tablet_stat_rpm),
                    value = rpm,
                    hero = true,
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.LocalGasStation,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    label = stringResource(R.string.tablet_stat_gross),
                    value = gross,
                    hero = true,
                )
            }
        }
    } else {
        StatsRow {
            SoftStatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.LocalShipping,
                tint = MaterialTheme.colorScheme.primaryContainer,
                label = stringResource(R.string.tablet_stat_loads),
                value = loadCount,
            )
            SoftStatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.Route,
                tint = LocalSemantics.current.successContainer,
                label = stringResource(R.string.tablet_stat_miles),
                value = miles,
            )
            SoftStatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.Speed,
                tint = LocalSemantics.current.warningContainer,
                label = stringResource(R.string.tablet_stat_rpm),
                value = rpm,
                hero = true,
            )
            SoftStatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.LocalGasStation,
                tint = MaterialTheme.colorScheme.primaryContainer,
                label = stringResource(R.string.tablet_stat_gross),
                value = gross,
                hero = true,
            )
        }
    }
}
