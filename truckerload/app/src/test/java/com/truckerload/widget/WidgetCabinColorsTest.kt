package com.truckerload.widget

import androidx.compose.ui.graphics.toArgb
import com.truckerload.presentation.theme.forestDarkColorScheme
import com.truckerload.presentation.theme.forestLightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCabinColorsTest {

    @Test
    fun forestPlate_isBrighterThanOldCabin() {
        val old = 0xFF12251C.toInt()
        assertTrue(luminance(WidgetCabinColors.Forest.bg) > luminance(old))
        assertEquals(WidgetCabinPalette.BG, WidgetCabinColors.Forest.bg)
        assertFalse(WidgetCabinColors.Forest.dynamic)
    }

    @Test
    fun forestLightPlate_usesDarkReadableText() {
        assertEquals(0xFF1A1A1A.toInt(), WidgetCabinColors.Forest.text)
        assertEquals(0xFF5C5C5C.toInt(), WidgetCabinColors.Forest.muted)
        assertTrue(luminance(WidgetCabinColors.Forest.bg) > luminance(WidgetCabinColors.ForestDark.bg))
        assertEquals(0xFFF4F3FA.toInt(), WidgetCabinColors.ForestDark.text)
    }

    @Test
    fun fromScheme_mapsSurfaceAndPrimary() {
        val scheme = forestDarkColorScheme()
        val colors = WidgetCabinColors.fromScheme(scheme, dynamic = true)
        assertTrue(colors.dynamic)
        assertEquals(scheme.surface.toArgb(), colors.bg)
        assertEquals(scheme.primary.toArgb(), colors.accent)
        assertEquals(scheme.onSurface.toArgb(), colors.text)
        assertEquals(scheme.primary.toArgb(), colors.actionBg)
        assertEquals(scheme.onPrimary.toArgb(), colors.actionLabel)
    }

    @Test
    fun fromScheme_light_mapsNearBlackBodyText() {
        val scheme = forestLightColorScheme()
        val colors = WidgetCabinColors.fromScheme(scheme, dynamic = false)
        assertFalse(colors.dynamic)
        assertEquals(scheme.onSurface.toArgb(), colors.text)
        assertEquals(scheme.primary.toArgb(), colors.accent)
    }

    private fun luminance(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return r + g + b
    }
}
