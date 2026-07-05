package com.truckerload.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.usecase.ForecastTrend
import com.truckerload.domain.usecase.WeekForecast
import com.truckerload.presentation.theme.LocalTruckColors

private val GreenAbove = Color(0xFF00E676)
private val YellowOnTrack = Color(0xFFFFCA28)
private val RedBelow = Color(0xFFFF3D57)

@Composable
fun ForecastCard(
    forecast: WeekForecast,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current
    val trendColor = when (forecast.trend) {
        ForecastTrend.ABOVE -> GreenAbove
        ForecastTrend.ON_TRACK -> YellowOnTrack
        ForecastTrend.BELOW -> RedBelow
    }
    val animatedProgress by animateFloatAsState(
        targetValue = forecast.progressPercent.coerceIn(0f, 1f),
        animationSpec = tween(500)
    )

    val message = when (forecast.trend) {
        ForecastTrend.ABOVE -> stringResource(R.string.forecast_above, forecast.deltaAmount)
        ForecastTrend.ON_TRACK -> stringResource(R.string.forecast_on_track)
        ForecastTrend.BELOW -> stringResource(R.string.forecast_below, -forecast.deltaAmount)
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tc.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.forecast_expected, forecast.basedOnWeeks),
                style = MaterialTheme.typography.labelMedium,
                color = tc.TextSecondary
            )
            Text(
                text = "$${String.format("%,.0f", forecast.expectedRate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.forecast_actual, forecast.currentRate),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .height(8.dp)
                    .padding(vertical = 4.dp),
                color = trendColor,
                trackColor = tc.Divider
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = trendColor
            )
            Text(
                text = stringResource(R.string.forecast_based_on_weeks, forecast.basedOnWeeks),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextLabel,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
