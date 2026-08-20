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
    val RpmGreen = Color(0xFF176B3A)
    val RpmYellow = Color(0xFF8A5800)
    val RpmRed = Color(0xFFB42318)
    val RpmGray = Color(0xFF3A5748)
    val TextMuted = Color(0xFF3A5748)
}
