package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.usecase.FuelAnalytics
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale

@Composable
fun FuelAnalyticsCard(
    analytics: FuelAnalytics,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tc.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.finance_fuel_analytics_title, analytics.periodLabel),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary
            )
            Text(
                text = stringResource(
                    R.string.finance_fuel_analytics_mpg,
                    String.format(Locale.getDefault(), "%.1f", analytics.avgMpg)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary
            )
            Text(
                text = stringResource(
                    R.string.finance_fuel_analytics_price_per_gallon,
                    String.format(Locale.getDefault(), "%.2f", analytics.avgPricePerGallon)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary
            )
            Text(
                text = stringResource(
                    R.string.finance_fuel_analytics_spent,
                    String.format(Locale.getDefault(), "%,.2f", analytics.totalSpent)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.AccentExpense
            )
            if (analytics.totalSavings > 0.0) {
                Text(
                    text = stringResource(
                        R.string.finance_fuel_analytics_savings,
                        String.format(Locale.getDefault(), "%,.2f", analytics.totalSavings)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.AccentProfit
                )
            }
            Text(
                text = stringResource(
                    R.string.finance_fuel_analytics_per_100_miles,
                    String.format(Locale.getDefault(), "%.2f", analytics.costPer100Miles)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextLabel
            )
        }
    }
}
