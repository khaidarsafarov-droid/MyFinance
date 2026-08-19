package com.truckerload.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneUiTokensTest {

    @Test
    fun cardCornersStayInOneUiRange() {
        assertTrue(OneUiTokens.isCornerInOneUiRange(OneUiTokens.CornerChip.value))
        assertTrue(OneUiTokens.isCornerInOneUiRange(OneUiTokens.CornerButton.value))
        assertTrue(OneUiTokens.isCornerInOneUiRange(OneUiTokens.CornerCard.value))
        assertTrue(OneUiTokens.isCornerInOneUiRange(OneUiTokens.CornerCardLarge.value))
        assertTrue(OneUiTokens.isCornerInOneUiRange(SoftUiDimens.CardRadius.value))
        assertFalse(OneUiTokens.isCornerInOneUiRange(12f))
        assertFalse(OneUiTokens.isCornerInOneUiRange(32f))
    }

    @Test
    fun overlayOledIfNeeded_replacesSurfacesOnly() {
        val base = darkColorScheme(
            primary = Color.Red,
            background = Color.DarkGray,
            surface = Color.Gray,
        )
        val same = overlayOledIfNeeded(base, oled = false)
        assertEquals(base.background, same.background)
        assertEquals(base.primary, same.primary)

        val oled = overlayOledIfNeeded(base, oled = true)
        assertEquals(SoftUiColors.BackgroundOled, oled.background)
        assertEquals(SoftUiColors.SurfaceOled, oled.surface)
        assertEquals(Color.Red, oled.primary)
    }
}
