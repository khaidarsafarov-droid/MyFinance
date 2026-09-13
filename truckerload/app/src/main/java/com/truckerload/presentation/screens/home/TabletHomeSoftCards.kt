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
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiColors
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
            .background(SoftUiColors.ForestPrimary),
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = gross,
                    style = AppTypography.HeroNumberOnDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = miles,
                        style = AppTypography.CaptionMuted.copy(color = onPrimaryMuted),
                        maxLines = 1,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = rpm,
                            style = AppTypography.HeroNumberCompact.copy(color = onPrimary),
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.home_period_avg_rpm_label),
                            style = AppTypography.CaptionMuted.copy(color = onPrimaryMuted),
                            maxLines = 1,
                        )
                    }
                }
            }
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
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowTint,
            )
            .clip(SoftUiShapes.Button)
            .background(SoftUiColors.ForestPrimary)
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
            color = Color.White,
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
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .clip(SoftUiShapes.Card)
            .background(SoftUiColors.Sage.copy(alpha = 0.45f))
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
    val rpm = computeRpm(load.totalRate, load.totalMiles)
    val stops = load.stopCount.takeIf { it > 0 } ?: (load.puCount + load.delCount)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
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
                MoneyFormat.formatNumber(load.totalMiles),
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
                color = SoftUiColors.ForestPrimary,
                modifier = Modifier
                    .clip(SoftUiShapes.Chip)
                    .background(SoftUiColors.Sage)
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
            .heightIn(min = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(AppIcons.Flag, contentDescription = null, tint = SoftUiColors.ForestAccent)
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
                    .background(SoftUiColors.Sage),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .background(SoftUiColors.ForestPrimary),
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
                color = SoftUiColors.ForestAccent,
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
                    tint = SoftUiColors.Sage,
                    label = stringResource(R.string.tablet_stat_loads),
                    value = loadCount,
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.Route,
                    tint = Color(0xFFD4F5EE),
                    label = stringResource(R.string.tablet_stat_miles),
                    value = miles,
                )
            }
            StatsRow {
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.Speed,
                    tint = Color(0xFFFFF3D6),
                    label = stringResource(R.string.tablet_stat_rpm),
                    value = rpm,
                    hero = true,
                )
                SoftStatCard(
                    modifier = Modifier.weight(1f),
                    icon = AppIcons.LocalGasStation,
                    tint = SoftUiColors.Sage,
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
                tint = SoftUiColors.Sage,
                label = stringResource(R.string.tablet_stat_loads),
                value = loadCount,
            )
            SoftStatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.Route,
                tint = Color(0xFFD4F5EE),
                label = stringResource(R.string.tablet_stat_miles),
                value = miles,
            )
            SoftStatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.Speed,
                tint = Color(0xFFFFF3D6),
                label = stringResource(R.string.tablet_stat_rpm),
                value = rpm,
                hero = true,
            )
            SoftStatCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.LocalGasStation,
                tint = SoftUiColors.Sage,
                label = stringResource(R.string.tablet_stat_gross),
                value = gross,
                hero = true,
            )
        }
    }
}
