package com.example.myfinance.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myfinance.data.WeeklyTotal
import com.example.myfinance.ui.theme.Amber400
import com.example.myfinance.ui.theme.Emerald400
import com.example.myfinance.ui.theme.Sky400

@Composable
fun AnalyticsScreen(
    weeklyTotals: List<WeeklyTotal>,
    modifier: Modifier = Modifier
) {
    val totals = weeklyTotals.fold(Triple(0.0, 0.0, 0.0)) { acc, w ->
        Triple(acc.first + w.gross, acc.second + w.netProfit, acc.third + w.diesel)
    }
    val sorted = weeklyTotals.sortedBy { it.date }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Analytics",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Financial overview",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                title = "Gross",
                value = formatCurrency(totals.first),
                color = Emerald400,
                icon = Icons.Default.MonetizationOn,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Net profit",
                value = formatCurrency(totals.second),
                color = Sky400,
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Diesel",
                value = formatCurrency(totals.third),
                color = Amber400,
                icon = Icons.Default.LocalGasStation,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Dynamics Over Time",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(12.dp))
        if (sorted.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No data yet. Add weekly totals to see the chart.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            SimpleLineChart(
                weeklyTotals = sorted,
                modifier = Modifier.fillMaxWidth().height(240.dp)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

@Composable
private fun SimpleLineChart(
    weeklyTotals: List<WeeklyTotal>,
    modifier: Modifier = Modifier
) {
    val maxG = weeklyTotals.maxOfOrNull { it.gross } ?: 1.0
    val maxP = weeklyTotals.maxOfOrNull { it.netProfit } ?: 1.0
    val maxD = weeklyTotals.maxOfOrNull { it.diesel } ?: 1.0
    val maxY = maxOf(maxG, maxP, maxD, 1.0)
    val n = weeklyTotals.size
    val stepX = if (n <= 1) 1f else 1f / (n - 1)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp)) {
            val w = size.width
            val h = size.height - 24f
            val baseY = h

            fun y(v: Double) = (baseY - (v / maxY * (h - 8f))).toFloat()

            val pathG = Path().apply {
                weeklyTotals.forEachIndexed { i, wt ->
                    val x = size.width * (i * stepX) + 8f
                    val yy = y(wt.gross)
                    if (i == 0) moveTo(x, yy) else lineTo(x, yy)
                }
            }
            val pathP = Path().apply {
                weeklyTotals.forEachIndexed { i, wt ->
                    val x = size.width * (i * stepX) + 8f
                    val yy = y(wt.netProfit)
                    if (i == 0) moveTo(x, yy) else lineTo(x, yy)
                }
            }
            val pathD = Path().apply {
                weeklyTotals.forEachIndexed { i, wt ->
                    val x = size.width * (i * stepX) + 8f
                    val yy = y(wt.diesel)
                    if (i == 0) moveTo(x, yy) else lineTo(x, yy)
                }
            }
            drawPath(pathG, Emerald400, style = Stroke(width = 4f, cap = StrokeCap.Round))
            drawPath(pathP, Sky400, style = Stroke(width = 4f, cap = StrokeCap.Round))
            drawPath(pathD, Amber400, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
    }
}
