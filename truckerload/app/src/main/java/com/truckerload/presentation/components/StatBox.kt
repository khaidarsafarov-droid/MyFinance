package com.truckerload.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.LocalTruckColors

/** Compact equal-height metric tile for load detail (and similar dense grids). */
@Composable
fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    hero: Boolean = false,
) {
    val tc = LocalTruckColors.current
    BentoGlassMetricCell(
        modifier = modifier,
        label = title,
        value = value,
        accent = if (hero) tc.TextNumbers else tc.AccentPrimary,
        hero = hero,
        compact = true,
    )
}
