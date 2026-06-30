package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.RouteStats
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun RouteStatsCard(
    route: RouteStats,
    rank: Int,
    bestRatePerMile: Double,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current
    val rankColor = when (rank) {
        1 -> tc.AccentPrimary
        2 -> tc.TextSecondary
        3 -> tc.AccentSecondary
        else -> tc.TextSecondary
    }
    val rateColor = when {
        route.ratePerMile >= 1.70 -> tc.AccentProfit
        route.ratePerMile >= 1.40 -> tc.AccentWarning
        else -> tc.AccentExpense
    }
    val progressPercent = if (bestRatePerMile > 0) (route.ratePerMile / bestRatePerMile).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tc.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleSmall,
                    color = rankColor,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .padding(4.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = route.routeKey,
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    text = stringResource(R.string.route_loads, route.totalLoads),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary
                )
                Text(
                    text = stringResource(R.string.route_avg_rate_per_trip, route.avgRate),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary
                )
                Text(
                    text = stringResource(R.string.route_avg_miles, route.avgMiles),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${String.format("%.2f", route.ratePerMile)}/mi",
                style = MaterialTheme.typography.labelMedium,
                color = rateColor
            )
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(top = 4.dp),
                color = rateColor,
                trackColor = tc.Divider
            )
        }
    }
}
