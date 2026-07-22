package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.StateRevenue
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale

@Composable
fun StateProfitabilityList(
    states: List<StateRevenue>,
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
                text = stringResource(R.string.state_list_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
            if (states.isEmpty()) {
                Text(
                    text = stringResource(R.string.state_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                states.forEachIndexed { index, sr ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${sr.state}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tc.TextPrimary,
                                fontFamily = FontFamily.SansSerif
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${formatMoney(sr.revenue)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = tc.AccentProfit,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = stringResource(R.string.state_list_trips_format, sr.trips),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tc.TextSecondary
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { sr.shareOfTotal },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            color = tc.AccentPrimary,
                            trackColor = tc.SurfaceSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun formatMoney(v: Double): String =
    if (v >= 1000 || v <= -1000) String.format(Locale.US, "%,.0f", v)
    else String.format(Locale.US, "%,.2f", v)
