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
    val heroText: Color,
    val heroTextMuted: Color,
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
    /** Gold button gradient. Text on the button is [goldOnGold]. */
    val goldStart: Color,
    val goldEnd: Color,
    val goldOnGold: Color,
    /** Small Premium tag. Dark uses a lighter gold on a brown chip. */
    val goldTagText: Color,
    val goldTagBg: Color,
    /** Gold mark that sits on the indigo hero. */
    val goldOnHero: Color,
) {
    /**
     * Label for a white chip or button. Light primary is indigo and clears AA on
     * white. Dark primary is champagne, which does not, so the label uses onPrimary.
     */
    fun labelOnWhite(): Color =
        if (ContrastRatios.passesAa(primary, Color.White)) primary else onPrimary
}

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
        heroText = SoftUiColors.HeroTextLight,
        heroTextMuted = SoftUiColors.HeroTextMutedLight,
        orange = SoftUiColors.OrangeLight,
        iconBlue = SoftUiColors.ForestPrimary,
        iconViolet = SoftUiColors.ForestPrimary,
        iconGreen = SemanticColors.Light.success,
        tagRelayFg = SoftUiColors.ForestPrimary,
        tagRelayBg = SoftUiColors.Sage,
        tagManualFg = SemanticColors.Light.success,
        tagManualBg = SemanticColors.Light.successContainer,
        tagBrokerFg = SoftUiColors.OnContainerLight,
        tagBrokerBg = Color(0xFFE6E8F0),
        shadow = Color(0x403F3D9E),
        goldStart = Color(0xFFE4D2A1),
        goldEnd = Color(0xFFC8A867),
        goldOnGold = Color(0xFF1D1708),
        goldTagText = Color(0xFF86542A),
        goldTagBg = Color(0xFFF6F0E4),
        goldOnHero = Color(0xFFC8A867),
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
        heroText = SoftUiColors.HeroTextDark,
        heroTextMuted = SoftUiColors.HeroTextMutedDark,
        orange = SoftUiColors.OrangeDark,
        iconBlue = SoftUiColors.OnContainerDark,
        iconViolet = SoftUiColors.ForestAccentDark,
        iconGreen = SemanticColors.Dark.success,
        tagRelayFg = SoftUiColors.OnContainerDark,
        tagRelayBg = SoftUiColors.SurfaceMutedDark,
        tagManualFg = SemanticColors.Dark.success,
        tagManualBg = SemanticColors.Dark.successContainer,
        tagBrokerFg = SoftUiColors.TextPrimaryDark,
        tagBrokerBg = Color(0xFF1E2438),
        shadow = Color(0x80000000),
        goldStart = Color(0xFFE4D2A1),
        goldEnd = Color(0xFFD6BC85),
        goldOnGold = Color(0xFF1D1708),
        goldTagText = Color(0xFFD6BC85),
        goldTagBg = Color(0xFF2C2618),
        goldOnHero = Color(0xFFD6BC85),
    )
}

val LocalBrandPalette = staticCompositionLocalOf { BrandPalettes.Light }
