package com.truckerload.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.truckerload.presentation.theme.BentoGlassMetricCell

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
        accent = MaterialTheme.colorScheme.primary,
    )
}
