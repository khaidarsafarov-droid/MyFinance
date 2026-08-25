package com.truckerload.presentation.screens.paycheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Paycheck
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.formatDateForDisplay

@Composable
fun PaycheckCard(
    paycheck: Paycheck,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = paycheck.weekLabel.ifBlank {
                        stringResource(
                            R.string.paycheck_week_fallback,
                            paycheck.weekNumber,
                            paycheck.year,
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
                Text(
                    text = MoneyFormat.formatCurrency(paycheck.netAmount, decimals = 2),
                    style = AppTypography.NumbersSmall,
                    color = tc.TextPrimary,
                )
            }
            Text(
                text = formatDateForDisplay(paycheck.addedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            paycheck.grossAmount?.takeIf { it > 0.0 && it != paycheck.netAmount }?.let { gross ->
                Text(
                    text = stringResource(
                        R.string.paycheck_gross_line,
                        MoneyFormat.formatCurrency(gross, decimals = 2),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            }
            paycheck.driverName?.takeIf { it.isNotBlank() }?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            }
            paycheck.sourceFileName?.takeIf { it.isNotBlank() }?.let { file ->
                Text(
                    text = file,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            }
        }
    }
}
