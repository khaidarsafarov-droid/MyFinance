package com.truckerload.presentation.screens.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.SoftTabletTwoPane
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.GoalCelebrationStore
import com.truckerload.presentation.components.WeeklyGoalSkeleton
import com.truckerload.presentation.theme.focusAfterNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGoalScreen() {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
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

    SoftAppPageScaffold(
        title = stringResource(R.string.goal_screen_title),
        subtitle = progress?.weekLabel,
    ) { padding ->
                if (uiState.isLoading || progress == null) {
            WeeklyGoalSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@SoftAppPageScaffold
        }

        BentoGlassScreenBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .focusAfterNavigate(key = "weekly-goal", enabled = true)
                    .padding(padding)
                    .verticalContentScroll()
                    .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SoftTabletTwoPane(
                    start = {
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
                    },
                    end = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            PaceInsightCard(progress = progress)
                            GoalPaceMetrics(progress = progress)
                        }
                    },
                )

                if (progress.targetAmount <= 0) {
                    Text(
                        text = stringResource(R.string.ux_goal_ownership_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                if (progress.targetAmount > 0 && progress.remainingAmount > 0) {
                    Text(
                        text = stringResource(
                            R.string.ux_contrast_remaining_of_goal,
                            MoneyFormat.formatCurrency(progress.currentGross),
                            MoneyFormat.formatCurrency(progress.targetAmount),
                            MoneyFormat.formatCurrency(progress.remainingAmount),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                if (progress.loadsCount > 0) {
                    Text(
                        text = stringResource(
                            R.string.goal_pace_period_hint,
                            formatActiveDays(progress.totalActiveDays),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
