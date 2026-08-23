package com.truckerload.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.ux.ActivationChecklist
import com.truckerload.domain.ux.ActivationStep
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding

@Composable
fun ActivationChecklistCard(
    checklist: ActivationChecklist,
    onStepClick: (ActivationStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (checklist.allDone) return
    val tc = LocalTruckColors.current
    BentoGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveHorizontalPadding(), vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.ux_activation_title),
                style = AppTypography.CardTitle,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(
                    R.string.ux_activation_progress,
                    checklist.completedCount,
                    checklist.totalCount,
                ),
                style = AppTypography.Subtitle,
                color = tc.TextSecondary,
            )
            LinearProgressIndicator(
                progress = { checklist.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = tc.AccentPrimary,
                trackColor = tc.TextSecondary.copy(alpha = 0.2f),
            )
            checklist.steps.forEach { (step, done) ->
                val label = stringResource(stepLabelRes(step))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!done) {
                                Modifier.clickable { onStepClick(step) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (done) "✓" else "○",
                        color = if (done) tc.AccentProfit else tc.TextSecondary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (done) tc.TextSecondary else tc.TextPrimary,
                        fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun stepLabelRes(step: ActivationStep): Int = when (step) {
    ActivationStep.ACCOUNT_READY -> R.string.ux_activation_step_account
    ActivationStep.PROFILE_SETUP -> R.string.ux_activation_step_profile
    ActivationStep.FIRST_LOAD -> R.string.ux_activation_step_load
    ActivationStep.WEEKLY_GOAL -> R.string.ux_activation_step_goal
    ActivationStep.FIRST_DIESEL -> R.string.ux_activation_step_diesel
}
