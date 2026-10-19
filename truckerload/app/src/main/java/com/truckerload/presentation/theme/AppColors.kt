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
    val RpmGreen = Color(0xFF3D6A55)
    val RpmYellow = Color(0xFF86542A)
    val RpmRed = Color(0xFF953C46)
    val RpmGray = Color(0xFF585A6E)
    val TextMuted = Color(0xFF585A6E)
}
