package com.truckerload.presentation.theme

import androidx.compose.ui.graphics.Color
import com.truckerload.domain.goal.PaceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticContrastTest {

    private val white = Color.White
    private val contentBg = SoftUiColors.ContentBg
    private val cabinBg = SoftUiColors.BackgroundDark

    @Test
    fun lightSemanticTextPassesAaOnWhiteAndContentBg() {
        val light = SemanticColors.Light
        listOf(light.success, light.warning, light.danger, light.neutral, light.hero).forEach { fg ->
            assertTrue("$fg on white", ContrastRatios.passesAa(fg, white))
            assertTrue("$fg on content bg", ContrastRatios.passesAa(fg, contentBg))
        }
    }

    @Test
    fun lightHeroPassesAaaOnWhite() {
        assertTrue(
            ContrastRatios.passesAaa(SemanticColors.Light.hero, white, largeText = true),
        )
        assertTrue(
            ContrastRatios.passesAaa(SemanticColors.Light.hero, contentBg, largeText = true),
        )
    }

    @Test
    fun darkSemanticTextPassesAaOnCabinBackground() {
        val dark = SemanticColors.Dark
        listOf(dark.success, dark.warning, dark.danger, dark.neutral, dark.hero).forEach { fg ->
            assertTrue("$fg on cabin", ContrastRatios.passesAa(fg, cabinBg))
        }
        assertTrue(ContrastRatios.passesAaa(dark.hero, cabinBg, largeText = true))
    }

    @Test
    fun mutedLavenderOnLightBackgroundFailsAa_regression() {
        assertTrue(
            ContrastRatios.contrast(SoftUiColors.ForestMuted, contentBg) < ContrastRatios.AA_NORMAL,
        )
        assertTrue(
            ContrastRatios.passesAa(SoftUiColors.TextSecondaryLight, contentBg),
        )
        assertTrue(
            ContrastRatios.passesAa(SoftUiColors.TextPrimaryLight, contentBg),
        )
        assertTrue(
            ContrastRatios.passesAa(white, SoftUiColors.ForestPrimary),
        )
    }

    @Test
    fun displayStylesUseTabularFigures() {
        assertEquals("tnum", Typography.displayLarge.fontFeatureSettings)
        assertEquals("tnum", Typography.displayMedium.fontFeatureSettings)
        assertEquals("tnum", Typography.headlineMedium.fontFeatureSettings)
    }

    @Test
    fun paceMapsToFourSemanticHues() {
        val light = SemanticColors.Light
        assertEquals(light.success, light.pace(PaceStatus.GOAL_MET))
        assertEquals(light.success, light.pace(PaceStatus.AHEAD))
        assertEquals(light.warning, light.pace(PaceStatus.ON_TRACK))
        assertEquals(light.danger, light.pace(PaceStatus.BEHIND))
    }
}
