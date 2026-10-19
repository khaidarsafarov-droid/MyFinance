package com.truckerload.presentation.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P2 Midnight & Gold. Body text and button text stay at or above WCAG AA (4.5).
 * Expected ratios are the published token-sheet figures.
 */
class MidnightGoldContrastTest {

    @Test
    fun lightPairsMeetAa() {
        val brand = BrandPalettes.Light
        val semantic = SemanticColors.Light
        assertAa(brand.textPrimary, brand.page, 15.92)
        assertAa(brand.textPrimary, brand.card, 17.07)
        assertAa(brand.textSecondary, brand.card, 6.60)
        assertAa(brand.onPrimary, brand.primary, 8.85)
        assertAa(brand.primary, brand.card, 8.63)
        assertAa(brand.heroText, brand.heroStart, 13.54)
        assertAa(brand.heroTextMuted, brand.heroStart, 9.75)
        assertAa(brand.goldOnHero, brand.heroEnd, 8.05)
        assertAa(semantic.success, semantic.successContainer, 5.19)
        assertAa(semantic.warning, semantic.warningContainer, 5.32)
        assertAa(semantic.danger, semantic.dangerContainer, 5.66)
        assertAa(semantic.success, Color.White, 6.19)
        assertAa(semantic.warning, Color.White, 6.33)
        assertAa(semantic.danger, Color.White, 6.97)
        assertAa(brand.goldOnGold, brand.goldEnd, 7.85)
        assertAa(brand.goldOnGold, brand.goldStart, 11.91)
        assertAa(AppColors.RpmGreen, Color.White, 6.19)
        assertAa(AppColors.RpmYellow, Color.White, 6.33)
        assertAa(AppColors.RpmRed, Color.White, 6.97)
        assertAa(brand.labelOnWhite(), Color.White, 8.85)
    }

    @Test
    fun darkPairsMeetAa() {
        val brand = BrandPalettes.Dark
        val semantic = SemanticColors.Dark
        assertAa(brand.textPrimary, brand.page, 15.96)
        assertAa(brand.textPrimary, brand.card, 14.63)
        assertAa(brand.textSecondary, brand.card, 7.27)
        assertAa(brand.onPrimary, brand.primary, 9.67)
        assertAa(brand.primary, brand.card, 9.45)
        assertAa(brand.heroText, brand.heroStart, 12.96)
        assertAa(brand.heroTextMuted, brand.heroStart, 8.24)
        assertAa(brand.goldOnHero, brand.heroEnd, 9.76)
        assertAa(semantic.success, semantic.successContainer, 7.09)
        assertAa(semantic.warning, semantic.warningContainer, 7.29)
        assertAa(semantic.danger, semantic.dangerContainer, 6.65)
        assertAa(brand.goldOnGold, brand.goldEnd, 9.67)
        assertAa(brand.goldOnGold, brand.goldStart, 11.91)
        assertAa(SoftUiColors.OnDangerDark, semantic.danger, 7.77)
        assertAa(brand.goldTagText, brand.goldTagBg, 8.15)
        assertAa(brand.labelOnWhite(), Color.White, 17.82)
    }

    private fun assertAa(foreground: Color, background: Color, expected: Double) {
        val ratio = ContrastRatios.contrast(foreground, background)
        assertTrue(
            "contrast $ratio expected about $expected for $foreground on $background",
            ratio >= ContrastRatios.AA_NORMAL && kotlin.math.abs(ratio - expected) < 0.25,
        )
    }
}
