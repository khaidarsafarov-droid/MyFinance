package com.truckerload.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Fallback RPM / status colors used outside [TruckColorPalette] CompositionLocal
 * (Glance XML, tests, rare non-theme call sites). Values match light [SemanticPalette]
 * so they stay WCAG AA on white.
 *
 * Prefer [TruckColorPalette.Success] / [Warning] / [Danger] / [Neutral] in Compose screens.
 */
object AppColors {
    val RpmGreen = Color(0xFF1A7A68)
    val RpmYellow = Color(0xFF9A6400)
    val RpmRed = Color(0xFFC4395A)
    val RpmGray = Color(0xFF5C5C5C)
    val TextMuted = Color(0xFF5C5C5C)
}
