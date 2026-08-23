package com.truckerload.presentation.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.domain.model.analytics.DailyData
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@Composable
fun TopRoutesBarChart(
    routes: List<RouteData>,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    if (routes.isEmpty()) {
        ChartEmptyState(modifier = modifier.height(160.dp))
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        routes.forEach { route ->
            RouteRow(route = route)
        }
    }
}

@Composable
private fun RouteRow(route: RouteData) {
    val tc = LocalTruckColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = route.route,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = tc.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = buildString {
                append(MoneyFormat.formatCurrency(route.gross))
                append(" • ")
                append(MoneyFormat.formatMiles(route.miles))
                append(" • ")
                append(MoneyFormat.formatRpm(route.rpm))
            },
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun DailyDistributionChart(
    dailyData: List<DailyData>,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    if (dailyData.isEmpty()) {
        ChartEmptyState(modifier = modifier.height(180.dp))
        return
    }
    val maxGross = dailyData.maxOf { it.gross }.coerceAtLeast(1.0)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        dailyData.forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = day.dayLabel,
                    modifier = Modifier.width(28.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftUiColors.SurfaceMuted),
                ) {
                    if (day.gross > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((day.gross / maxGross).toFloat().coerceIn(0.04f, 1f))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SoftUiColors.ForestAccent, SoftUiColors.ForestMuted),
                                    ),
                                ),
                        )
                    }
                }
                Text(
                    text = MoneyFormat.formatCurrency(day.gross),
                    modifier = Modifier
                        .width(72.dp)
                        .padding(start = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextPrimary,
                )
            }
        }
    }
}

