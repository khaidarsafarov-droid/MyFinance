package com.truckerload.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Layout tokens — soft UI aliases. */
object DarkGlassTokens {
    val BackgroundTop = SoftUiColors.BackgroundLight
    val BackgroundBottom = SoftUiColors.BackgroundLight
    val Surface = SoftUiColors.SurfaceLight
    val SurfaceMetric = SoftUiColors.SurfaceLight
    val SurfaceList = SoftUiColors.SurfaceLight
    val Border = Color(0xFFE5E7EB)
    val AccentBlue = SoftUiColors.PurpleEnd
    val AccentPurple = SoftUiColors.PurpleStart
    val TextPrimary = SoftUiColors.TextPrimaryLight
    val TextSecondary = SoftUiColors.TextSecondaryLight
    val TextMuted = Color(0xFF9CA3AF)
    val TextNumbers = SoftUiColors.TextPrimaryLight
    val Success = AppColors.RpmGreen
    val Warning = AppColors.RpmYellow
    val Error = AppColors.RpmRed
    val ShadowGlow = SoftUiColors.ShadowTint
    val CornerRadius = SoftUiDimens.CardRadius
    val CellRadius = SoftUiDimens.ChipRadius
    val BorderWidth = 1.dp
    val CardElevation = AppElevation.Card
    val BlurRadius = 0.dp
    val SurfaceSolidTop = SoftUiColors.SurfaceLight
    val SurfaceSolidBottom = SoftUiColors.SurfaceLight
}

typealias NeoGlassColors = TruckColorPalette
val LocalColors = LocalTruckColors

object NeoGlassPalette {
    val GlowAlpha = 0.15f
    val SkyTop = SoftUiColors.BackgroundLight
    val SkyBottom = SoftUiColors.BackgroundLight
    val Primary = SoftUiColors.PurpleStart
    val Secondary = SoftUiColors.SkyBlueEnd
    val Success = AppColors.RpmGreen
    val Warning = AppColors.RpmYellow
    val Error = AppColors.RpmRed
    val ShadowSoft = SoftUiColors.ShadowTint
    val GlassStroke = Color(0xFFE5E7EB)
    val Surface = SoftUiColors.SurfaceLight
}

@Composable
fun glowColor(accent: Color = LocalColors.current.AccentPrimary): Color =
    accent.copy(alpha = NeoGlassPalette.GlowAlpha)
