package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.domain.usecase.FuelAnalytics
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun FuelAnalyticsCard(
    analytics: FuelAnalytics,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current
    val locale = LocalConfiguration.current.locales[0]
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
                text = if (DisplayUnits.isMetric) {
                    val economy = MeasurementUnits.displayFuelEconomy(analytics.avgMpg)
                    stringResource(
                        R.string.finance_fuel_analytics_l_per_100,
                        if (economy == null) "—" else String.format(locale, "%.1f", economy),
                    )
                } else {
                    stringResource(
                        R.string.finance_fuel_analytics_mpg,
                        String.format(locale, "%.1f", analytics.avgMpg),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary
            )
            Text(
                text = if (DisplayUnits.isMetric) {
                    stringResource(
                        R.string.finance_fuel_analytics_price_per_liter,
                        String.format(
                            locale,
                            "%.2f",
                            MeasurementUnits.displayPricePerVolume(analytics.avgPricePerGallon),
                        ),
                    )
                } else {
                    stringResource(
                        R.string.finance_fuel_analytics_price_per_gallon,
                        String.format(locale, "%.2f", analytics.avgPricePerGallon),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary
            )
            Text(
                text = stringResource(
                    R.string.finance_fuel_analytics_spent,
                    String.format(locale, "%,.2f", analytics.totalSpent)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.AccentExpense
            )
            if (analytics.totalSavings > 0.0) {
                Text(
                    text = stringResource(
                        R.string.finance_fuel_analytics_savings,
                        String.format(locale, "%,.2f", analytics.totalSavings)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.AccentProfit
                )
            }
            Text(
                text = if (DisplayUnits.isMetric) {
                    stringResource(
                        R.string.finance_fuel_analytics_per_100_km,
                        String.format(
                            locale,
                            "%.2f",
                            MeasurementUnits.displayCostPer100(analytics.costPer100Miles),
                        ),
                    )
                } else {
                    stringResource(
                        R.string.finance_fuel_analytics_per_100_miles,
                        String.format(locale, "%.2f", analytics.costPer100Miles),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextLabel
            )
        }
    }
}
