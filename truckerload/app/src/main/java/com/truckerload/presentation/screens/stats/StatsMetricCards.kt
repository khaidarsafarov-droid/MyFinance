package com.truckerload.presentation.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.SoftCard
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
internal fun HeroNetProfitCard(netProfit: Double, change: Double?, sparkline: List<Float>, onClick: () -> Unit) {
    val tc = LocalTruckColors.current
    SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.stats_label_net_profit),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
            Text("$${formatMoney(netProfit)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TrendText(change = change, goodWhenUp = false)
            Sparkline(values = sparkline, color = tc.AccentPrimary)
        }
    }
}

@Composable
internal fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    change: Double?,
    goodWhenUp: Boolean,
    sparkline: List<Float>?,
    onClick: (() -> Unit)? = null
) {
    val tc = LocalTruckColors.current
    SoftCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = tc.TextLabel
                    )
                }
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = tc.TextPrimary)
            TrendText(change = change, goodWhenUp = goodWhenUp)
            sparkline?.let {
                val sparkColor = when {
                    change == null -> tc.TextSecondary
                    goodWhenUp && change >= 0 -> tc.AccentProfit
                    goodWhenUp && change < 0 -> tc.AccentExpense
                    !goodWhenUp && change > 0 -> tc.AccentExpense
                    else -> tc.AccentProfit
                }
                Sparkline(it, sparkColor)
            }
        }
    }
}

@Composable
internal fun RpmCard(modifier: Modifier = Modifier, rpm: Double, target: Double) {
    val tc = LocalTruckColors.current
    val progress = (rpm / target).toFloat().coerceIn(0f, 1f)
    SoftCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.stats_label_avg_rpm), color = tc.TextSecondary)
            Text("$${"%.2f".format(rpm)}/mi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.stats_rpm_goal, target),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(tc.ProgressTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(Brush.horizontalGradient(listOf(tc.AccentPrimary, tc.AccentSecondary)))
                )
            }
            Text(
                stringResource(R.string.stats_rpm_low_warning),
                style = MaterialTheme.typography.labelSmall,
                color = tc.AccentWarning
            )
        }
    }
}

@Composable
internal fun Sparkline(values: List<Float>, color: Color) {
    if (values.isEmpty()) return
    val max = values.maxOrNull() ?: 1f
    val min = values.minOrNull() ?: 0f
    val range = (max - min).takeIf { it > 0f } ?: 1f
    Canvas(modifier = Modifier.fillMaxWidth().height(30.dp)) {
        val path = Path()
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - ((v - min) / range) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal fun TrendText(change: Double?, goodWhenUp: Boolean) {
    if (change == null) return
    val tc = LocalTruckColors.current
    val up = change >= 0
    val positiveOutcome = if (goodWhenUp) up else !up
    val color = if (positiveOutcome) tc.AccentProfit else tc.AccentExpense
    val arrow = if (up) "▲" else "▼"
    Text(
        text = stringResource(
            R.string.stats_trend_vs_prev_week,
            arrow,
            formatPct(change),
            stringResource(R.string.stats_vs_prev_week)
        ),
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
internal fun SectionTitle(text: String) {
    val tc = LocalTruckColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = tc.TextSecondary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
internal fun AdvancedStats(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val tc = LocalTruckColors.current
    Column(modifier = modifier.background(tc.Background), content = content)
}
