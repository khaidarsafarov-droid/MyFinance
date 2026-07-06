package com.truckerload.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.screens.goal.GoalViewModel
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.adaptiveHorizontalPadding

@Composable
fun HomeGoalSection(
    onOpenGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadRepository = LocalLoadRepository.current
    val goalStore = LocalWeeklyProfitGoalStore.current
    val context = LocalContext.current
    val viewModel: GoalViewModel = viewModel(
        factory = GoalViewModel.Factory(loadRepository, goalStore, context),
    )
    val progress by viewModel.goalProgress.collectAsState()

    progress?.let { p ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = adaptiveHorizontalPadding())
                .clickable(onClick = onOpenGoal),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.goal_metric_remaining),
                    value = MoneyFormat.formatCurrency(p.remainingAmount),
                    accent = FinanceCockpitColors.DieselAccent,
                )
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.goal_metric_days_left),
                    value = p.daysRemainingInWeek.toString(),
                    accent = if (p.paceStatus == PaceStatus.BEHIND) {
                        LocalTruckColors.current.AccentExpense
                    } else {
                        LocalTruckColors.current.TextSecondary
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.goal_metric_actual_pace),
                    value = MoneyFormat.formatCurrency(p.actualDailyYield),
                    accent = LocalTruckColors.current.AccentProfit,
                )
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.goal_metric_daily_needed),
                    value = MoneyFormat.formatCurrency(p.dailyTargetNeeded),
                    accent = LocalTruckColors.current.AccentInfo,
                )
            }
        }
    }
}
