package com.truckerload.presentation.screens.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.goal.DayPlanRow
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.presentation.components.WeeklyGoalSkeleton
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.focusAfterNavigate
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.GoalCelebrationStore
import androidx.compose.ui.platform.LocalContext

@Composable
fun WeeklyGoalScreen(motionSettled: Boolean = true) {
    val context = LocalContext.current
    val viewModel: GoalViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress = uiState.progress
    LaunchedEffect(motionSettled) {
        if (motionSettled) viewModel.onMotionSettled()
    }
    LaunchedEffect(
        motionSettled,
        progress?.paceStatus,
        progress?.weekNumber,
        progress?.year,
        progress?.targetAmount,
    ) {
        if (!motionSettled) return@LaunchedEffect
        val current = progress ?: return@LaunchedEffect
        if (current.paceStatus == PaceStatus.GOAL_MET &&
            !GoalCelebrationStore.wasCelebrated(
                context,
                current.weekNumber,
                current.year,
                current.targetAmount.cents / 100.0,
            )
        ) {
            GoalCelebrationStore.markCelebrated(
                context,
                current.weekNumber,
                current.year,
                current.targetAmount.cents / 100.0,
            )
            FeedbackManager.onGoalReached()
        }
    }

    val brand = LocalBrandPalette.current
    // Phone chrome already applies the status-bar inset. A second scaffold inset
    // would leave a page-colored band above the hero.
    val windowInsets = if (useNavigationRail()) {
        WindowInsets.safeDrawing
    } else {
        WindowInsets(0, 0, 0, 0)
    }
    Scaffold(containerColor = brand.page, contentWindowInsets = windowInsets) { padding ->
        if (uiState.isLoading || progress == null) {
            WeeklyGoalSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brand.page)
                    .padding(padding),
            )
            return@Scaffold
        }
        WeeklyGoalContent(
            progress = progress,
            dayPlan = uiState.dayPlan,
            motionSettled = motionSettled,
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
            modifier = Modifier
                .fillMaxSize()
                .focusAfterNavigate(key = "weekly-goal", enabled = motionSettled)
                .padding(padding),
        )
    }
}

@Composable
internal fun WeeklyGoalContent(
    progress: WeeklyGoalProgress,
    dayPlan: List<DayPlanRow>?,
    motionSettled: Boolean,
    isEditingGoal: Boolean,
    goalInput: String,
    goalError: String?,
    isSavingGoal: Boolean,
    suggestedGoals: List<Double>,
    onStartEdit: () -> Unit,
    onGoalInputChange: (String) -> Unit,
    onSaveGoal: () -> Unit,
    onCancelEdit: () -> Unit,
    onSuggestedGoal: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val bottomGap = if (useNavigationRail()) 24.dp else 96.dp
    Column(
        modifier = modifier
            .background(brand.page)
            .verticalContentScroll(),
    ) {
        GoalHeroCard(
            progress = progress,
            isEditingGoal = isEditingGoal,
            goalInput = goalInput,
            goalError = goalError,
            isSavingGoal = isSavingGoal,
            suggestedGoals = suggestedGoals,
            onStartEdit = onStartEdit,
            onGoalInputChange = onGoalInputChange,
            onSaveGoal = onSaveGoal,
            onCancelEdit = onCancelEdit,
            onSuggestedGoal = onSuggestedGoal,
            motionSettled = motionSettled,
        )
        if (!progress.targetAmount.isPositive()) {
            Text(
                text = stringResource(R.string.ux_goal_ownership_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = brand.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        WeeklyGoalDayPlanCard(
            progress = progress,
            dayPlan = dayPlan,
            motionSettled = motionSettled,
        )
        Spacer(Modifier.height(bottomGap))
    }
}
