package com.truckerload.presentation.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Documents search used the border token as hint text. That pair is about 1:1
 * on the midnight field. Hint, typed text, and the gold icon/cursor must clear AA.
 */
class SearchFieldContrastTest {

    @Test
    fun darkFieldHintIsMutedGoldAndClearsAa() {
        val field = SoftUiColors.SurfaceMutedDark
        val hint = readableOnField(
            field,
            listOf(BrandPalettes.Dark.heroTextMuted, BrandPalettes.Dark.textSecondary),
            BrandPalettes.Dark.textPrimary,
        )
        val icon = readableOnField(
            field,
            listOf(BrandPalettes.Dark.primary, BrandPalettes.Dark.heroTextMuted, BrandPalettes.Dark.textPrimary),
            BrandPalettes.Dark.textPrimary,
        )
        assertEquals(BrandPalettes.Dark.heroTextMuted, hint)
        assertEquals(BrandPalettes.Dark.primary, icon)
        assertTrue(ContrastRatios.passesAa(hint, field))
        assertTrue(ContrastRatios.passesAa(BrandPalettes.Dark.textPrimary, field))
        assertTrue(ContrastRatios.passesAa(icon, field))
        assertFalse(
            "outline placeholder must stay rejected on the navy field",
            ContrastRatios.passesAa(SoftUiColors.CardBorderDark, field),
        )
    }

    @Test
    fun lightFieldHintFallsBackToSecondaryText() {
        val field = SoftUiColors.ShellBg
        val hint = readableOnField(
            field,
            listOf(BrandPalettes.Light.heroTextMuted, BrandPalettes.Light.textSecondary),
            BrandPalettes.Light.textPrimary,
        )
        val icon = readableOnField(
            field,
            listOf(BrandPalettes.Light.primary, BrandPalettes.Light.heroTextMuted, BrandPalettes.Light.textPrimary),
            BrandPalettes.Light.textPrimary,
        )
        assertEquals(BrandPalettes.Light.textSecondary, hint)
        assertEquals(BrandPalettes.Light.primary, icon)
        assertTrue(ContrastRatios.passesAa(hint, field))
        assertTrue(ContrastRatios.passesAa(BrandPalettes.Light.textPrimary, field))
        assertTrue(ContrastRatios.passesAa(icon, field))
        assertFalse(ContrastRatios.passesAa(SoftUiColors.CardBorder, field))
    }

    @Test
    fun oledFieldKeepsGoldHintAndInput() {
        val field = SoftUiColors.SurfaceMutedOled
        val hint = readableOnField(
            field,
            listOf(BrandPalettes.Dark.heroTextMuted, BrandPalettes.Dark.textSecondary),
            BrandPalettes.Dark.textPrimary,
        )
        assertEquals(BrandPalettes.Dark.heroTextMuted, hint)
        assertTrue(ContrastRatios.passesAa(hint, field))
        assertTrue(ContrastRatios.passesAa(BrandPalettes.Dark.textPrimary, field))
        assertTrue(ContrastRatios.passesAa(BrandPalettes.Dark.primary, field))
    }
}
