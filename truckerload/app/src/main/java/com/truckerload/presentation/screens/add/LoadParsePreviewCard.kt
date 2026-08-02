package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import java.util.Locale

/**
 * Reciprocity: show a real parsed preview before the user commits to Save.
 */
@Composable
fun LoadParsePreviewCard(
    preview: Load?,
    isParsing: Boolean,
    parseHint: String?,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    if (!isParsing && preview == null && parseHint == null) return

    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.add_load_preview_title),
                style = AppTypography.CardTitle,
                color = tc.TextPrimary,
            )
            when {
                isParsing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(2.dp),
                            strokeWidth = 2.dp,
                            color = tc.AccentPrimary,
                        )
                        Text(
                            stringResource(R.string.add_load_preview_parsing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                    }
                }
                preview != null -> {
                    Text(
                        text = stringResource(R.string.add_load_preview_gift),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.AccentProfit,
                    )
                    val trip = preview.tripId.ifBlank { "—" }
                    Text(
                        text = stringResource(R.string.add_load_preview_trip, trip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val route = listOf(preview.pointA, preview.pointB)
                        .filter { it.isNotBlank() }
                        .joinToString(" → ")
                        .ifBlank { preview.route }
                    if (route.isNotBlank()) {
                        Text(route, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            MoneyFormat.formatCurrency(preview.totalRate),
                            style = AppTypography.AccentNumber.copy(color = tc.AccentProfit),
                        )
                        Text(
                            "${MoneyFormat.formatNumber(preview.totalMiles)} mi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextPrimary,
                        )
                        val rpm = if (preview.totalMiles > 0) preview.totalRate / preview.totalMiles else 0.0
                        Text(
                            String.format(Locale.US, "$%.2f/mi", rpm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                    }
                }
                parseHint != null -> {
                    Text(
                        text = parseHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
            }
        }
    }
}
