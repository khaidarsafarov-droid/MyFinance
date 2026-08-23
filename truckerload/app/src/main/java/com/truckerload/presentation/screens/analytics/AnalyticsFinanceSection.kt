package com.truckerload.presentation.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.analytics.PeriodFinance
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import java.util.Locale

/**
 * Money summary for the selected reporting period: settlement in, diesel out,
 * and what the fuel discount saved. All values come from the driver's own entries.
 */
@Composable
fun AnalyticsFinanceSection(
    finance: PeriodFinance,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.analytics_finance_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )

            if (!finance.hasData) {
                Text(
                    text = stringResource(R.string.analytics_finance_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                return@Column
            }

            Text(
                text = MoneyFormat.formatCurrency(finance.netProfit),
                style = AppTypography.HeroNumber,
                color = if (finance.netProfit >= 0) tc.AccentProfit else tc.AccentExpense,
            )
            Text(
                text = stringResource(R.string.analytics_finance_net_profit),
                style = MaterialTheme.typography.labelMedium,
                color = tc.TextSecondary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FinanceStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analytics_finance_paycheck),
                    value = MoneyFormat.formatCurrency(finance.paycheckTotal),
                    valueColor = FinanceCockpitColors.SalaryAccent,
                )
                FinanceStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.analytics_finance_diesel),
                    value = MoneyFormat.formatCurrency(finance.dieselTotal),
                    valueColor = tc.AccentExpense,
                )
            }

            if (finance.dieselGallons > 0.0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinanceStat(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.analytics_finance_gallons),
                        value = String.format(Locale.US, "%,.0f", finance.dieselGallons),
                        valueColor = tc.TextPrimary,
                    )
                    FinanceStat(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.analytics_finance_avg_price),
                        value = finance.avgPricePerGallon
                            ?.let { String.format(Locale.US, "$%.2f", it) }
                            ?: "—",
                        valueColor = tc.TextPrimary,
                    )
                }
            }

            if (finance.dieselSavings > 0.0) {
                Text(
                    text = stringResource(
                        R.string.analytics_finance_savings,
                        MoneyFormat.formatCurrency(finance.dieselSavings),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.AccentProfit,
                )
            }
        }
    }
}

@Composable
private fun FinanceStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tc.TextSecondary,
        )
    }
}
