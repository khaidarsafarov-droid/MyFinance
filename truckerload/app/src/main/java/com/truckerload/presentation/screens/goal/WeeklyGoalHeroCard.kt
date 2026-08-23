package com.truckerload.presentation.screens.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.presentation.components.GoalProgressRing
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.MoneyFormat

@Composable
internal fun GoalHeroCard(
    progress: WeeklyGoalProgress,
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
                    AppIcons.Flag,
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
                        AppIcons.Edit,
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
