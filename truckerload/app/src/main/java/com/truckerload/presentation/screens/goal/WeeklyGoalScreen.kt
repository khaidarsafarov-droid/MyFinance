package com.truckerload.presentation.screens.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.presentation.components.AnimatedCircularProgress
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalWeeklyProfitGoalStore
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.isTablet
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.GoalCelebrationStore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGoalScreen() {
    val tc = LocalTruckColors.current
    val loadRepository = LocalLoadRepository.current
    val goalStore = LocalWeeklyProfitGoalStore.current
    val context = LocalContext.current
    val viewModel: GoalViewModel = viewModel(
        factory = GoalViewModel.Factory(loadRepository, goalStore, context)
    )
    val uiState by viewModel.uiState.collectAsState()
    val progress = uiState.progress
    LaunchedEffect(
        progress?.paceStatus,
        progress?.weekNumber,
        progress?.year,
        progress?.targetAmount,
    ) {
        val current = progress ?: return@LaunchedEffect
        if (current.paceStatus == PaceStatus.GOAL_MET &&
            !GoalCelebrationStore.wasCelebrated(
                context,
                current.weekNumber,
                current.year,
                current.targetAmount,
            )
        ) {
            GoalCelebrationStore.markCelebrated(
                context,
                current.weekNumber,
                current.year,
                current.targetAmount,
            )
            FeedbackManager.onGoalReached()
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.goal_screen_title),
                            color = tc.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        progress?.let {
                            Text(
                                it.weekLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = tc.TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading || progress == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FinanceCockpitColors.SalaryAccent)
            }
            return@Scaffold
        }

        BentoGlassScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoalHeroCard(
                progress = progress,
                isEditingGoal = uiState.isEditingGoal,
                goalInput = uiState.goalInput,
                goalError = uiState.goalError,
                isSavingGoal = uiState.isSavingGoal,
                onStartEdit = viewModel::startEditingGoal,
                onGoalInputChange = viewModel::onGoalInputChange,
                onSaveGoal = viewModel::saveGoal,
                onCancelEdit = viewModel::cancelEditingGoal
            )

            PaceInsightCard(progress = progress)

            if (progress.loadsCount > 0) {
                Text(
                    text = stringResource(
                        R.string.goal_pace_period_hint,
                        formatActiveDays(progress.totalActiveDays)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceCockpitColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.goal_metric_remaining),
                    value = formatMoney(progress.remainingAmount),
                    accent = FinanceCockpitColors.DieselAccent
                )
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.goal_metric_days_left),
                    value = progress.daysRemainingInWeek.toString(),
                    accent = FinanceCockpitColors.SalaryAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.goal_metric_actual_pace),
                    value = formatMoney(progress.actualDailyYield),
                    accent = BentoGlassTheme.GoalGradientEnd,
                    highlight = true
                )
                BentoGlassMetricCell(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.goal_metric_daily_needed),
                    value = formatMoney(progress.dailyTargetNeeded),
                    accent = tc.AccentProfit
                )
            }

            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun GoalHeroCard(
    progress: com.truckerload.domain.goal.WeeklyGoalProgress,
    isEditingGoal: Boolean,
    goalInput: String,
    goalError: String?,
    isSavingGoal: Boolean,
    onStartEdit: () -> Unit,
    onGoalInputChange: (String) -> Unit,
    onSaveGoal: () -> Unit,
    onCancelEdit: () -> Unit
) {
    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        useHeroGradient = true,
        content = {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = null,
                    tint = FinanceCockpitColors.SalaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.goal_weekly_target),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FinanceCockpitColors.TextPrimary
                )
            }
            if (!isEditingGoal) {
                IconButton(onClick = onStartEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.goal_edit_target))
                }
            }
        }

        if (isEditingGoal) {
            OutlinedTextField(
                value = goalInput,
                onValueChange = onGoalInputChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text(stringResource(R.string.goal_target_amount)) },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = goalError != null,
                supportingText = goalError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinanceCockpitColors.SalaryAccent
                ),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = onSaveGoal,
                    modifier = Modifier.weight(1f),
                    enabled = !isSavingGoal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinanceCockpitColors.SalaryAccent
                    )
                ) {
                    if (isSavingGoal) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(stringResource(R.string.common_save))
                    }
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))

            AnimatedCircularProgress(
                progressPercent = progress.progressPercent,
                gross = progress.currentGross,
                goal = progress.targetAmount,
                paceStatus = progress.paceStatus,
                size = if (isTablet()) 260.dp else 220.dp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.goal_completed),
                style = MaterialTheme.typography.bodySmall,
                color = FinanceCockpitColors.TextMuted,
                textAlign = TextAlign.Center
            )
        }
        }
    })
}

@Composable
private fun PaceInsightCard(progress: com.truckerload.domain.goal.WeeklyGoalProgress) {
    val tc = LocalTruckColors.current
    val (title, body, accent) = when (progress.paceStatus) {
        PaceStatus.GOAL_MET -> Triple(
            stringResource(R.string.goal_pace_met_title),
            stringResource(R.string.goal_pace_met_body, formatMoney(progress.currentGross)),
            FinanceCockpitColors.NetProfitStart
        )
        PaceStatus.AHEAD -> Triple(
            stringResource(R.string.goal_pace_ahead_title),
            stringResource(
                R.string.goal_pace_ahead_body,
                formatMoney(progress.actualDailyYield),
                formatMoney(progress.dailyTargetNeeded)
            ),
            FinanceCockpitColors.NetProfitStart
        )
        PaceStatus.ON_TRACK -> Triple(
            stringResource(R.string.goal_pace_on_track_title),
            stringResource(
                R.string.goal_pace_on_track_body,
                formatMoney(progress.actualDailyYield),
                formatMoney(progress.dailyTargetNeeded),
                progress.daysRemainingInWeek
            ),
            FinanceCockpitColors.SalaryAccent
        )
        PaceStatus.BEHIND -> Triple(
            stringResource(R.string.goal_pace_behind_title),
            stringResource(
                R.string.goal_pace_behind_body,
                formatMoney(progress.dailyTargetNeeded),
                formatMoney(progress.remainingAmount)
            ),
            tc.AccentWarning
        )
    }

    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accent.copy(alpha = 0.35f),
        content = {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = accent, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(body, color = FinanceCockpitColors.TextSecondary, lineHeight = 22.sp)
            }
        }
    )
}


private fun formatMoney(value: Double): String =
    String.format(Locale.US, "$%,.0f", value)

private fun formatActiveDays(days: Double): String =
    days.toLong().coerceAtLeast(1L).toString()
