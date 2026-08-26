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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Load
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
    subtitle: String,
    onAddLoad: () -> Unit,
    filterContent: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Icon(
            imageVector = AppIcons.LocalShipping,
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
                style = AppTypography.CaptionMuted.copy(color = Color.White.copy(alpha = 0.88f)),
            )
            Text(
                text = gross,
                style = AppTypography.HeroNumberOnDark,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = subtitle,
                style = AppTypography.CaptionMuted.copy(color = Color.White.copy(alpha = 0.88f)),
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
            color = SoftUiColors.TextSecondaryLight,
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
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
        Text(
            text = stringResource(if (weeklyGoal > 0) R.string.nav_weekly_goal else R.string.ux_next_set_goal),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
        )
        // Detailed ring/amounts live on the Goal tab only.
        Text(
            text = stringResource(R.string.nav_weekly_goal),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = SoftUiColors.ForestAccent,
        )
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
                    tint = Color(0xFFEEEDFF),
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
                    tint = Color(0xFFEEEDFF),
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
                tint = Color(0xFFEEEDFF),
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
                tint = Color(0xFFEEEDFF),
                label = stringResource(R.string.tablet_stat_gross),
                value = gross,
                hero = true,
            )
        }
    }
}
