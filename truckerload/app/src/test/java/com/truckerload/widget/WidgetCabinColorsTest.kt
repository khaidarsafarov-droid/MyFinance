package com.truckerload.widget

import androidx.compose.ui.graphics.toArgb
import com.truckerload.presentation.theme.forestDarkColorScheme
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
    fun fromScheme_mapsSurfaceAndPrimary() {
        val scheme = forestDarkColorScheme()
        val colors = WidgetCabinColors.fromScheme(scheme, dynamic = true)
        assertTrue(colors.dynamic)
        assertEquals(scheme.surface.toArgb(), colors.bg)
        assertEquals(scheme.primary.toArgb(), colors.accent)
        assertEquals(scheme.onSurface.toArgb(), colors.text)
    }

    private fun luminance(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return r + g + b
    }
}
