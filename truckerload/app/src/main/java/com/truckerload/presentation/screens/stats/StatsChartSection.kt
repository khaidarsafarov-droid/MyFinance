package com.truckerload.presentation.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.SoftCard
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

internal data class LinePoint(
    val label: String,
    val revenue: Float,
    val expense: Float
)

@Composable
internal fun RevenueChartCard(points: List<LinePoint>) {
    val tc = LocalTruckColors.current
    var selectedIndex by remember { mutableIntStateOf(3) }
    val yMax = max(points.maxOf { it.revenue }, 12000f)
    val breakEven = points.map { it.expense }.average().toFloat()
    SoftCard {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                stringResource(R.string.stats_chart_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures { tap ->
                                val slot = size.width / (points.size - 1).coerceAtLeast(1)
                                selectedIndex = (tap.x / slot).roundToInt().coerceIn(0, points.lastIndex)
                            }
                        }
                ) {
                    val slot = size.width / (points.size - 1).coerceAtLeast(1)
                    val toY: (Float) -> Float = { v ->
                        val normalized = (v / yMax).coerceIn(0f, 1f)
                        size.height - (normalized * (size.height - 12.dp.toPx())) - 6.dp.toPx()
                    }
                    val revenuePath = Path()
                    val expensePath = Path()
                    val revenueAreaPath = Path()
                    points.forEachIndexed { i, p ->
                        val x = i * slot
                        val ry = toY(p.revenue)
                        val ey = toY(p.expense)
                        if (i == 0) {
                            revenuePath.moveTo(x, ry)
                            expensePath.moveTo(x, ey)
                            revenueAreaPath.moveTo(x, size.height)
                            revenueAreaPath.lineTo(x, ry)
                        } else {
                            revenuePath.lineTo(x, ry)
                            expensePath.lineTo(x, ey)
                            revenueAreaPath.lineTo(x, ry)
                        }
                    }
                    revenueAreaPath.lineTo((points.lastIndex * slot), size.height)
                    revenueAreaPath.close()
                    drawPath(
                        path = revenueAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(tc.AccentPrimary.copy(alpha = 0.33f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = revenuePath,
                        color = tc.AccentPrimary,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = expensePath,
                        color = tc.AccentWarning,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    val breakY = toY(breakEven)
                    drawLine(
                        color = tc.TextSecondary.copy(alpha = 0.45f),
                        start = Offset(0f, breakY),
                        end = Offset(size.width, breakY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                    points.forEachIndexed { i, p ->
                        val x = i * slot
                        val selected = i == selectedIndex
                        drawCircle(
                            color = if (selected) tc.AccentSecondary else tc.AccentPrimary,
                            radius = if (selected) 5.dp.toPx() else 3.dp.toPx(),
                            center = Offset(x, toY(p.revenue))
                        )
                        drawCircle(
                            color = if (selected) tc.AccentWarning else tc.AccentWarning.copy(alpha = 0.75f),
                            radius = if (selected) 5.dp.toPx() else 3.dp.toPx(),
                            center = Offset(x, toY(p.expense))
                        )
                    }
                }
                val tip = points[selectedIndex]
                SoftCard(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(8, 6) },
                    contentPadding = 8.dp,
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            stringResource(R.string.stats_chart_peak_profit, tip.label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.stats_chart_revenue_value, formatMoney(tip.revenue.toDouble())),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            stringResource(R.string.stats_chart_expense_value, formatMoney(tip.expense.toDouble())),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            stringResource(R.string.stats_chart_diesel_value, "950"),
                            style = MaterialTheme.typography.labelSmall,
                            color = tc.AccentWarning
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { Text(it.label, style = MaterialTheme.typography.labelSmall, color = tc.TextSecondary) }
            }
        }
    }
}

internal fun buildIllustrativeChart(ui: StatsUiState): List<LinePoint> {
    val gross = ui.totalGross.takeIf { it > 0 } ?: 12817.0
    val diesel = ui.totalDiesel.takeIf { it > 0 } ?: 3412.0
    val revenueBase = (gross / 7.0).toFloat()
    val expenseBase = (diesel / 7.0).toFloat()
    val multipliers = when (ui.statsPeriod) {
        StatsPeriod.WEEK -> listOf(0.72f, 0.85f, 1.03f, 1.48f, 0.96f, 1.22f, 1.10f)
        StatsPeriod.MONTH -> listOf(0.90f, 1.00f, 1.08f, 1.28f, 1.12f, 1.18f, 1.14f)
        StatsPeriod.YEAR -> listOf(0.95f, 1.02f, 1.10f, 1.22f, 1.06f, 1.12f, 1.18f)
    }
    val monthLabel = monthShortLabel(ui.calendarMonth).lowercase(Locale.getDefault())
    return (1..7).mapIndexed { i, day ->
        LinePoint(
            label = "$day $monthLabel",
            revenue = (revenueBase * multipliers[i] * 4.1f),
            expense = (expenseBase * multipliers[(i + 1) % multipliers.size] * 2.1f)
        )
    }
}
