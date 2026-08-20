package com.truckerload.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

/**
 * Горизонтальный прогресс цели недели — отличается от кругового графика на главной.
 */
@Composable
fun GoalLinearProgress(
    progressPercent: Float,
    currentGross: Double,
    targetAmount: Double,
    remainingAmount: Double,
    paceStatus: PaceStatus,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val tc = LocalTruckColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "goalLinearProgress",
    )
    val trackColor = cs.onPrimary.copy(alpha = 0.2f)
    val fillColor = tc.pace(paceStatus)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                R.string.goal_linear_progress_summary,
                MoneyFormat.formatCurrency(currentGross),
                MoneyFormat.formatCurrency(targetAmount),
            ),
            style = AppTypography.Subtitle.copy(color = cs.onPrimary.copy(alpha = 0.85f)),
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = fillColor,
            trackColor = trackColor,
        )
        Text(
            text = stringResource(
                R.string.goal_linear_percent_remaining,
                progressPercent.toInt(),
                MoneyFormat.formatCurrency(remainingAmount.coerceAtLeast(0.0)),
            ),
            style = AppTypography.CardTitle.copy(
                color = cs.onPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
