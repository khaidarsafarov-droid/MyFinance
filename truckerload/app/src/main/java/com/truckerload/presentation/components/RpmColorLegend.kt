package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RpmColorLegend(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tc = LocalTruckColors.current
    val rpmStore = LocalRpmThresholdsStore.current
    val thresholds by rpmStore.thresholds.collectAsStateWithLifecycle()
    val min = thresholds.minProfit
    val target = thresholds.targetProfit

    val items = listOf(
        tc.AccentExpense to stringResource(R.string.rpm_legend_red, min),
        tc.AccentWarning to stringResource(R.string.rpm_legend_yellow, min, target),
        tc.AccentProfit to stringResource(R.string.rpm_legend_green, target),
    )

    if (compact) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { (color, label) ->
                LegendChip(color = color, label = label)
            }
        }
    } else {
        androidx.compose.foundation.layout.Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { (color, label) ->
                LegendChip(color = color, label = label)
            }
        }
    }
}

@Composable
private fun LegendChip(color: androidx.compose.ui.graphics.Color, label: String) {
    val tc = LocalTruckColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
    }
}
