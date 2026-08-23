package com.truckerload.presentation.screens.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@Composable
internal fun GoalPaceMetrics(progress: WeeklyGoalProgress) {
    val tc = LocalTruckColors.current
    val daysAccent = tc.pace(progress.paceStatus)
    val paceMatched = kotlin.math.abs(progress.actualDailyYield - progress.dailyTargetNeeded) < 1.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BentoGlassMetricCell(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.goal_metric_remaining),
            value = MoneyFormat.formatCurrency(progress.remainingAmount),
            accent = FinanceCockpitColors.DieselAccent,
        )
        BentoGlassMetricCell(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.goal_metric_days_left),
            value = progress.daysRemainingInWeek.toString(),
            accent = daysAccent,
        )
    }

    if (paceMatched) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth(), useCream = true) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.goal_metric_pace_matched),
                    style = AppTypography.CardTitle,
                )
                Text(
                    text = stringResource(
                        R.string.goal_metric_pace_matched_hint,
                    ) + " · " + MoneyFormat.formatCurrency(progress.actualDailyYield) + stringResource(R.string.per_day_suffix),
                    style = AppTypography.Subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.goal_metric_actual_pace),
                value = MoneyFormat.formatCurrency(progress.actualDailyYield),
                accent = if (progress.actualDailyYield >= progress.dailyTargetNeeded) {
                    tc.AccentProfit
                } else {
                    tc.AccentExpense
                },
                highlight = true,
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.goal_metric_daily_needed),
                value = MoneyFormat.formatCurrency(progress.dailyTargetNeeded),
                accent = tc.AccentInfo,
            )
        }
    }
}

@Composable
internal fun PaceInsightCard(progress: WeeklyGoalProgress) {
    val tc = LocalTruckColors.current
    val (title, body, accent) = when (progress.paceStatus) {
        PaceStatus.GOAL_MET -> Triple(
            stringResource(R.string.goal_pace_met_title),
            stringResource(R.string.goal_pace_met_short, MoneyFormat.formatCurrency(progress.currentGross)),
            tc.Success,
        )
        PaceStatus.AHEAD -> Triple(
            stringResource(R.string.goal_pace_ahead_title),
            stringResource(
                R.string.goal_pace_ahead_short,
                MoneyFormat.formatCurrency(progress.actualDailyYield),
                MoneyFormat.formatCurrency(progress.dailyTargetNeeded),
            ),
            tc.Success,
        )
        PaceStatus.ON_TRACK -> Triple(
            stringResource(R.string.goal_pace_on_track_title),
            stringResource(
                R.string.goal_pace_on_track_short,
                MoneyFormat.formatCurrency(progress.actualDailyYield),
                progress.daysRemainingInWeek,
            ),
            tc.Warning,
        )
        PaceStatus.BEHIND -> Triple(
            stringResource(R.string.goal_pace_behind_title),
            stringResource(
                R.string.goal_pace_behind_short,
                MoneyFormat.formatCurrency(progress.dailyTargetNeeded),
                MoneyFormat.formatCurrency(progress.remainingAmount),
            ),
            tc.TextPrimary,
        )
    }

    val paceIcon = when (progress.paceStatus) {
        PaceStatus.GOAL_MET, PaceStatus.AHEAD -> AppIcons.CheckCircle
        PaceStatus.ON_TRACK -> AppIcons.Timeline
        PaceStatus.BEHIND -> AppIcons.Timeline
    }

    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        useCream = true,
        content = {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = paceIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(title, style = AppTypography.CardTitle.copy(color = accent))
                }
                Spacer(Modifier.height(6.dp))
                Text(body, style = AppTypography.Body)
            }
        }
    )
}

internal fun formatActiveDays(days: Double): String =
    days.toLong().coerceAtLeast(1L).toString()
