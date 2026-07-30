package com.truckerload.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.domain.goal.PaceStatus
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

@Composable
fun GoalProgressRing(
    progressPercent: Float,
    paceStatus: PaceStatus,
    expectedProgressPercent: Float,
    centerLabel: String,
    centerSubLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = UiDimens.GoalProgressRingSize,
    strokeWidth: Dp = 12.dp,
    animate: Boolean = true,
    onDarkBackground: Boolean = false,
) {
    val tc = LocalTruckColors.current
    val cs = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = if (animate) progressPercent.coerceIn(0f, 100f) else progressPercent,
        animationSpec = tween(durationMillis = 300),
        label = "goalProgress"
    )
    val animatedExpected by animateFloatAsState(
        targetValue = if (animate) expectedProgressPercent.coerceIn(0f, 100f) else expectedProgressPercent,
        animationSpec = tween(durationMillis = 300),
        label = "expectedProgress"
    )

    val trackColor = if (onDarkBackground) cs.onPrimary.copy(alpha = 0.2f) else tc.ProgressTrack
    val gradientStart = tc.AccentPrimary
    val gradientEnd = tc.AccentProfit
    val paceMarkerColor = when (paceStatus) {
        PaceStatus.BEHIND -> tc.AccentWarning
        else -> tc.AccentProfit
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = size.toPx() - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(diameter, diameter)
            val startAngle = 135f
            val sweepTotal = 270f

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val progressSweep = sweepTotal * (animatedProgress / 100f)
            if (progressSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to gradientStart,
                        0.5f to gradientEnd,
                        1f to gradientEnd
                    ),
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            val paceSweep = sweepTotal * (animatedExpected / 100f)
            if (paceSweep > 0f) {
                val paceAngle = Math.toRadians((startAngle + paceSweep).toDouble())
                val radius = diameter / 2f
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                val markerCenter = Offset(
                    center.x + radius * kotlin.math.cos(paceAngle).toFloat(),
                    center.y + radius * kotlin.math.sin(paceAngle).toFloat()
                )
                drawCircle(color = paceMarkerColor, radius = stroke * 0.35f, center = markerCenter)
                drawCircle(color = Color.White, radius = stroke * 0.18f, center = markerCenter)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                ),
                color = if (onDarkBackground) cs.onPrimary else tc.TextPrimary,
            )
            Text(
                text = centerSubLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onDarkBackground) cs.onPrimary.copy(alpha = 0.75f) else tc.TextSecondary,
            )
        }
    }
}
