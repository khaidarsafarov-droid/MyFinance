package com.truckerload.presentation.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.DarkGlassGradients
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import java.util.Locale

@Composable
fun AnimatedCircularProgress(
    progressPercent: Float,
    gross: Double,
    goal: Double,
    paceStatus: PaceStatus = PaceStatus.ON_TRACK,
    modifier: Modifier = Modifier,
    size: Dp = UiDimens.HomeProgressRingSize,
    strokeWidth: Dp = 12.dp,
    showPercent: Boolean = true,
    onDarkBackground: Boolean = true,
) {
    val tc = LocalTruckColors.current
    val cs = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent.coerceIn(0f, 100f),
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "ringProgress",
    )
    val animatedGross by animateFloatAsState(
        targetValue = gross.toFloat(),
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "ringGross",
    )

    val ringBrush = DarkGlassGradients.progressRing

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = size.toPx() - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = if (onDarkBackground) {
                    cs.onPrimary.copy(alpha = 0.2f)
                } else {
                    tc.ProgressTrack
                },
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            val sweep = 360f * (animatedProgress / 100f)
            if (sweep > 0f) {
                drawArc(
                    brush = ringBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatUsd(animatedGross.toDouble()),
                style = if (onDarkBackground) AppTypography.NumbersOnDark else AppTypography.NumbersLarge,
            )
            if (goal > 0) {
                Text(
                    text = stringResource(R.string.widget_goal_out_of, formatUsd(goal)),
                    style = if (onDarkBackground) {
                        AppTypography.Subtitle.copy(color = cs.onPrimary.copy(alpha = 0.7f))
                    } else {
                        AppTypography.Subtitle
                    },
                )
            }
            if (showPercent) {
                Text(
                    text = "${animatedProgress.toInt()}%",
                    style = if (onDarkBackground) {
                        AppTypography.CardTitle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = cs.onPrimary,
                        )
                    } else {
                        AppTypography.AccentNumber.copy(fontSize = 18.sp)
                    },
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun formatUsd(value: Double): String =
    String.format(Locale.US, "$%,.0f", value)
