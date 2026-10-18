package com.truckerload.presentation.screens.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.ux.UxMotivation
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.UiDimens

@Composable
internal fun WeeklyGoalEditForm(
    goalInput: String,
    goalError: String?,
    isSavingGoal: Boolean,
    suggestedGoals: List<Double>,
    onGoalInputChange: (String) -> Unit,
    onSaveGoal: () -> Unit,
    onCancelEdit: () -> Unit,
    onSuggestedGoal: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
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
            singleLine = true,
        )
        if (suggestedGoals.isNotEmpty()) {
            Text(
                text = stringResource(R.string.ux_goal_suggested_label),
                style = MaterialTheme.typography.labelMedium,
                color = brand.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestedGoals.take(3).forEach { amount ->
                    OutlinedButton(
                        onClick = { onSuggestedGoal(amount) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = UxMotivation.formatSuggestedGoal(amount),
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancelEdit,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.common_cancel))
            }
            Button(
                onClick = onSaveGoal,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                enabled = !isSavingGoal,
            ) {
                if (isSavingGoal) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(UiDimens.IconInline),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}
