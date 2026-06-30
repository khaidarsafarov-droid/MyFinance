package com.truckerload.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.FinanceCockpitColors

@Composable
fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    BentoGlassMetricCell(
        modifier = modifier,
        label = title,
        value = value,
        accent = FinanceCockpitColors.TextPrimary
    )
}
