package com.truckerload.presentation.screens.goal

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import com.truckerload.presentation.components.LocalOpenDrawer
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.presentation.components.GoalProgressRing
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.ForestScreenTitle
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.GoalCelebrationStore
import com.truckerload.presentation.utils.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGoalScreen() {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val openDrawer = LocalOpenDrawer.current
    val viewModel: GoalViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                        ForestScreenTitle(stringResource(R.string.goal_screen_title))
                        progress?.let {
                            Text(it.weekLabel, style = AppTypography.Subtitle)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.common_menu), tint = tc.TextPrimary)
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
                suggestedGoals = uiState.suggestedGoals,
                onStartEdit = viewModel::startEditingGoal,
                onGoalInputChange = viewModel::onGoalInputChange,
                onSaveGoal = viewModel::saveGoal,
                onCancelEdit = viewModel::cancelEditingGoal,
                onSuggestedGoal = viewModel::applySuggestedGoal,
            )

            if (progress.targetAmount <= 0) {
                Text(
                    text = stringResource(R.string.ux_goal_ownership_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalTruckColors.current.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            PaceInsightCard(progress = progress)

            if (progress.targetAmount > 0 && progress.remainingAmount > 0) {
                Text(
                    text = stringResource(
                        R.string.ux_contrast_remaining_of_goal,
                        MoneyFormat.formatCurrency(progress.currentGross),
                        MoneyFormat.formatCurrency(progress.targetAmount),
                        MoneyFormat.formatCurrency(progress.remainingAmount),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalTruckColors.current.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            if (progress.loadsCount > 0) {
                Text(
                    text = stringResource(
                        R.string.goal_pace_period_hint,
                        formatActiveDays(progress.totalActiveDays)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalTruckColors.current.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            GoalPaceMetrics(progress = progress)

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
    suggestedGoals: List<Double> = emptyList(),
    onStartEdit: () -> Unit,
    onGoalInputChange: (String) -> Unit,
    onSaveGoal: () -> Unit,
    onCancelEdit: () -> Unit,
    onSuggestedGoal: (Double) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = UiDimens.GoalHeroMinHeight),
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
                    modifier = Modifier.size(UiDimens.IconList)
                )
                Text(
                    text = stringResource(R.string.goal_weekly_target),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onPrimary,
                )
            }
            if (!isEditingGoal) {
                IconButton(onClick = onStartEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.goal_edit_target),
                        tint = cs.onPrimary.copy(alpha = 0.7f),
                    )
                }
            }
        }

        if (isEditingGoal) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.surface)
                    .padding(16.dp),
            ) {
            OutlinedTextField(
                value = goalInput,
                onValueChange = onGoalInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goal_target_amount)) },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = goalError != null,
                supportingText = goalError?.let { { Text(it) } },
                colors = AppTextFieldDefaults.outlined(),
                singleLine = true
            )
            if (suggestedGoals.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.ux_goal_suggested_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestedGoals.take(3).forEach { amount ->
                        OutlinedButton(
                            onClick = { onSuggestedGoal(amount) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(MoneyFormat.formatCurrency(amount), maxLines = 1)
                        }
                    }
                }
            }
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
                ) {
                    if (isSavingGoal) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(UiDimens.IconInline),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(stringResource(R.string.common_save))
                    }
                }
            }
            }
        } else {
            Spacer(Modifier.height(16.dp))

            val expectedPercent = if (progress.targetAmount > 0) {
                (progress.expectedGrossByNow / progress.targetAmount * 100).toFloat().coerceIn(0f, 100f)
            } else {
                0f
            }
            GoalProgressRing(
                progressPercent = progress.progressPercent,
                paceStatus = progress.paceStatus,
                expectedProgressPercent = expectedPercent,
                centerLabel = "${progress.progressPercent.toInt()}%",
                centerSubLabel = MoneyFormat.formatCurrency(progress.currentGross),
                onDarkBackground = true,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.goal_linear_progress_summary,
                    MoneyFormat.formatCurrency(progress.currentGross),
                    MoneyFormat.formatCurrency(progress.targetAmount),
                ),
                style = AppTypography.Subtitle.copy(color = cs.onPrimary.copy(alpha = 0.85f)),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.goal_linear_percent_remaining,
                    progress.progressPercent.toInt(),
                    MoneyFormat.formatCurrency(progress.remainingAmount.coerceAtLeast(0.0)),
                ),
                style = AppTypography.CardTitle.copy(
                    color = cs.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        }
    })
}

@Composable
private fun GoalPaceMetrics(progress: com.truckerload.domain.goal.WeeklyGoalProgress) {
    val tc = LocalTruckColors.current
    val daysAccent = when (progress.paceStatus) {
        PaceStatus.BEHIND -> tc.AccentExpense
        else -> tc.TextSecondary
    }
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
private fun PaceInsightCard(progress: com.truckerload.domain.goal.WeeklyGoalProgress) {
    val tc = LocalTruckColors.current
    val (title, body, accent) = when (progress.paceStatus) {
        PaceStatus.GOAL_MET -> Triple(
            stringResource(R.string.goal_pace_met_title),
            stringResource(R.string.goal_pace_met_short, MoneyFormat.formatCurrency(progress.currentGross)),
            FinanceCockpitColors.NetProfitStart,
        )
        PaceStatus.AHEAD -> Triple(
            stringResource(R.string.goal_pace_ahead_title),
            stringResource(
                R.string.goal_pace_ahead_short,
                MoneyFormat.formatCurrency(progress.actualDailyYield),
                MoneyFormat.formatCurrency(progress.dailyTargetNeeded),
            ),
            FinanceCockpitColors.NetProfitStart,
        )
        PaceStatus.ON_TRACK -> Triple(
            stringResource(R.string.goal_pace_on_track_title),
            stringResource(
                R.string.goal_pace_on_track_short,
                MoneyFormat.formatCurrency(progress.actualDailyYield),
                progress.daysRemainingInWeek,
            ),
            FinanceCockpitColors.SalaryAccent,
        )
        PaceStatus.BEHIND -> Triple(
            stringResource(R.string.ux_loss_goal_behind_title),
            stringResource(
                R.string.ux_loss_goal_behind_body,
                MoneyFormat.formatCurrency(progress.remainingAmount),
                progress.daysRemainingInWeek,
                MoneyFormat.formatCurrency(progress.dailyTargetNeeded),
            ),
            tc.AccentWarning,
        )
    }

    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        useCream = true,
        content = {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(title, style = AppTypography.CardTitle.copy(color = accent))
                Spacer(Modifier.height(6.dp))
                Text(body, style = AppTypography.Body)
            }
        }
    )
}


private fun formatActiveDays(days: Double): String =
    days.toLong().coerceAtLeast(1L).toString()
