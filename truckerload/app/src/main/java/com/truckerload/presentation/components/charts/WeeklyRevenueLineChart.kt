package com.truckerload.presentation.components.charts

import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShader
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.truckerload.domain.model.analytics.WeekData
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import java.util.Locale

@Composable
fun WeeklyRevenueLineChart(
    weeks: List<WeekData>,
    selectedIndex: Int?,
    onWeekSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    if (weeks.isEmpty()) {
        ChartEmptyState(modifier = modifier.height(200.dp))
        return
    }

    val entries = remember(weeks) { weeks.map { it.gross.toFloat() } }
    val labels = remember(weeks) { weeks.map { it.label } }
    val model = remember(entries) { entryModelOf(*entries.toTypedArray()) }
    val cs = MaterialTheme.colorScheme
    val lineColor = cs.primary
    val lineArgb = lineColor.toArgb()
    val fillTopArgb = cs.primary.copy(alpha = 0.25f).toArgb()
    val fillShader = remember(fillTopArgb) {
        DynamicShader { _, left, top, right, bottom ->
            LinearGradient(
                left,
                top,
                left,
                bottom,
                intArrayOf(fillTopArgb, Color.Transparent.toArgb()),
                null,
                Shader.TileMode.CLAMP,
            )
        }
    }

    val bottomFormatter = remember(labels) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            labels.getOrNull(value.toInt()) ?: ""
        }
    }

    ProvideChartStyle(m3ChartStyle()) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(500)),
        ) {
            Chart(
            chart = lineChart(
                lines = listOf(
                    LineChart.LineSpec(
                        lineColor = lineArgb,
                        lineBackgroundShader = fillShader,
                        point = com.patrykandpatrick.vico.core.component.shape.ShapeComponent(
                            shape = Shapes.pillShape,
                            color = lineArgb,
                        ),
                        pointSizeDp = 8f,
                    )
                ),
                spacing = 24.dp,
            ),
            model = model,
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(end = 40.dp),
            startAxis = rememberStartAxis(
                valueFormatter = AxisValueFormatter { value, _ ->
                    String.format(Locale.US, "$%,.0f", value)
                },
                guideline = null,
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = bottomFormatter,
                guideline = null,
            ),
            chartScrollState = rememberChartScrollState(),
            isZoomEnabled = false,
        )
        }
    }

    selectedIndex?.let { index ->
        weeks.getOrNull(index)?.let { week ->
            Text(
                text = stringResource(
                    R.string.analytics_chart_week_detail,
                    week.label,
                    MoneyFormat.formatCurrency(week.gross),
                    week.loadCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ChartEmptyState(modifier: Modifier = Modifier) {
    val tc = LocalTruckColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.analytics_chart_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
        )
    }
}

