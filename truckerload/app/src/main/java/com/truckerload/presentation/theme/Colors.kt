package com.truckerload.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Airy Soft UI semantic colors. */
typealias NeoGlassColors = TruckColorPalette

val LocalColors = LocalTruckColors

object NeoGlassPalette {
    val GlowAlpha = 0.28f

    val SkyTop = Color(0xFFE0E7FF)
    val SkyBottom = Color(0xFFF5F3FF)

    val LightBackground = Color(0xFFF5F3FF)
    val LightSurface = Color(0xFFFFFFFF)
    val LightCard = Color(0xFFFFFFFF)

    val DarkBackground = Color(0xFF1E1B4B)
    val DarkSurface = Color(0xFF2D2A5E)
    val DarkCard = Color(0xFF2D2A5E)

    val Primary = Color(0xFF3B82F6)
    val Secondary = Color(0xFF8B5CF6)
    val Tertiary = Color(0xFF6366F1)
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val ShadowSoft = Color(0x1A64748B)
}

@Composable
fun glowColor(accent: Color = LocalColors.current.AccentPrimary): Color =
    accent.copy(alpha = NeoGlassPalette.GlowAlpha)
