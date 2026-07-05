package com.truckerload.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Layout tokens — aliases for light Soft UI. */
object DarkGlassTokens {
    val BackgroundTop = AppColors.White
    val BackgroundBottom = AppColors.White
    val Surface = AppColors.White
    val SurfaceMetric = AppColors.White
    val SurfaceList = AppColors.White
    val Border = AppColors.BorderGray
    val AccentBlue = AppColors.DarkTeal
    val AccentPurple = AppColors.Orange
    val TextPrimary = AppColors.TextPrimary
    val TextSecondary = AppColors.TextSecondary
    val TextMuted = AppColors.TextMuted
    val TextNumbers = AppColors.TextPrimary
    val Success = AppColors.RpmGreen
    val Warning = AppColors.RpmYellow
    val Error = AppColors.RpmRed
    val ShadowGlow = AppColors.DarkTeal08
    val CornerRadius = 24.dp
    val CellRadius = 16.dp
    val BorderWidth = 1.dp
    val CardElevation = AppElevation.Card
    val BlurRadius = 0.dp
    val SurfaceSolidTop = AppColors.White
    val SurfaceSolidBottom = AppColors.White
}

typealias NeoGlassColors = TruckColorPalette
val LocalColors = LocalTruckColors

object NeoGlassPalette {
    val GlowAlpha = 0.15f
    val SkyTop = AppColors.White
    val SkyBottom = AppColors.White
    val Primary = AppColors.DarkTeal
    val Secondary = AppColors.Orange
    val Success = AppColors.RpmGreen
    val Warning = AppColors.RpmYellow
    val Error = AppColors.RpmRed
    val ShadowSoft = AppColors.DarkTeal08
    val GlassStroke = AppColors.BorderGray
    val Surface = AppColors.White
}

@Composable
fun glowColor(accent: Color = LocalColors.current.AccentPrimary): Color =
    accent.copy(alpha = NeoGlassPalette.GlowAlpha)
