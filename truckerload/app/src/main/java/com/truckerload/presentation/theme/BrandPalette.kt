package com.truckerload.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand tokens for cards, chips, and the floating bar.
 * Independent of Material dynamic color so a wallpaper accent cannot
 * recolor these surfaces.
 */
data class BrandPalette(
    val page: Color,
    val card: Color,
    val cardBorder: Color,
    val borderStrong: Color,
    val container: Color,
    val onContainer: Color,
    val primary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val orange: Color,
    val iconBlue: Color,
    val iconViolet: Color,
    val iconGreen: Color,
    val tagRelayFg: Color,
    val tagRelayBg: Color,
    val tagManualFg: Color,
    val tagManualBg: Color,
    val tagBrokerFg: Color,
    val tagBrokerBg: Color,
    val shadow: Color,
)

object BrandPalettes {
    val Light = BrandPalette(
        page = SoftUiColors.ContentBg,
        card = SoftUiColors.SurfaceLight,
        cardBorder = SoftUiColors.CardBorder,
        borderStrong = SoftUiColors.SageBorderStrong,
        container = SoftUiColors.Sage,
        onContainer = SoftUiColors.OnContainerLight,
        primary = SoftUiColors.ForestPrimary,
        onPrimary = Color.White,
        textPrimary = SoftUiColors.TextPrimaryLight,
        textSecondary = SoftUiColors.TextSecondaryLight,
        textTertiary = SoftUiColors.TextTertiaryLight,
        heroStart = SoftUiColors.HeroStartLight,
        heroEnd = SoftUiColors.HeroEndLight,
        orange = SoftUiColors.OrangeLight,
        iconBlue = Color(0xFF1F55B5),
        iconViolet = SoftUiColors.ForestPrimary,
        iconGreen = Color(0xFF1D6B5C),
        tagRelayFg = Color(0xFF4A43D1),
        tagRelayBg = Color(0xFFECEAFF),
        tagManualFg = Color(0xFF1D6B5C),
        tagManualBg = Color(0xFFDDF3EE),
        tagBrokerFg = Color(0xFF1F55B5),
        tagBrokerBg = Color(0xFFE3EEFF),
        shadow = Color(0x405B54E6),
    )

    val Dark = BrandPalette(
        page = SoftUiColors.BackgroundDark,
        card = SoftUiColors.SurfaceDark,
        cardBorder = SoftUiColors.CardBorderDark,
        borderStrong = SoftUiColors.BorderStrongDark,
        container = SoftUiColors.SurfaceMutedDark,
        onContainer = SoftUiColors.OnContainerDark,
        primary = SoftUiColors.ForestAccentDark,
        onPrimary = SoftUiColors.OnForestAccentDark,
        textPrimary = SoftUiColors.TextPrimaryDark,
        textSecondary = SoftUiColors.TextSecondaryDark,
        textTertiary = SoftUiColors.TextTertiaryDark,
        heroStart = SoftUiColors.HeroStartDark,
        heroEnd = SoftUiColors.HeroEndDark,
        orange = SoftUiColors.OrangeDark,
        iconBlue = Color(0xFF8DB8FF),
        iconViolet = SoftUiColors.ForestAccentDark,
        iconGreen = Color(0xFF6FDCC4),
        tagRelayFg = Color(0xFFCFCBFF),
        tagRelayBg = Color(0xFF2E2A66),
        tagManualFg = Color(0xFF6FDCC4),
        tagManualBg = Color(0xFF123A33),
        tagBrokerFg = Color(0xFF8DB8FF),
        tagBrokerBg = Color(0xFF152D52),
        shadow = Color(0x80000000),
    )
}

val LocalBrandPalette = staticCompositionLocalOf { BrandPalettes.Light }
