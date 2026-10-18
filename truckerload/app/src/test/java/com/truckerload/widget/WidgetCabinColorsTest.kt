package com.truckerload.widget

import androidx.compose.ui.graphics.toArgb
import com.truckerload.presentation.theme.BrandPalettes
import com.truckerload.presentation.theme.forestDarkColorScheme
import com.truckerload.presentation.theme.forestLightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCabinColorsTest {

    @Test
    fun forestPlate_usesBrandCardNotDynamicColor() {
        val old = 0xFF12251C.toInt()
        assertTrue(luminance(WidgetCabinColors.Forest.bg) > luminance(old))
        assertEquals(BrandPalettes.Light.card.toArgb(), WidgetCabinColors.Forest.bg)
        assertEquals(0xFFFFFFFF.toInt(), WidgetCabinColors.Forest.bg)
        assertFalse(WidgetCabinColors.Forest.dynamic)
    }

    @Test
    fun forestDarkPlate_usesIndigoCabinAndHeader() {
        assertEquals(BrandPalettes.Dark.card.toArgb(), WidgetCabinColors.ForestDark.bg)
        assertEquals(0xFF17173A.toInt(), WidgetCabinColors.ForestDark.bg)
        assertEquals(0xFF3A33C9.toInt(), WidgetCabinColors.ForestDark.headerStart)
        assertEquals(0xFF19189A.toInt(), WidgetCabinColors.ForestDark.headerEnd)
    }

    @Test
    fun forestLight_headerIsIndigoAndBodyTextIsReadable() {
        assertEquals(BrandPalettes.Light.textPrimary.toArgb(), WidgetCabinColors.Forest.text)
        assertEquals(BrandPalettes.Light.textSecondary.toArgb(), WidgetCabinColors.Forest.muted)
        assertEquals(0xFF2B25A1.toInt(), WidgetCabinColors.Forest.headerStart)
        assertEquals(0xFF0C0F6C.toInt(), WidgetCabinColors.Forest.headerEnd)
        assertEquals(0xFF0B7A3E.toInt(), WidgetCabinColors.Forest.rpmFg)
        assertEquals(0xFFDDF5E8.toInt(), WidgetCabinColors.Forest.rpmBg)
        assertTrue(luminance(WidgetCabinColors.Forest.bg) > luminance(WidgetCabinColors.ForestDark.bg))
    }

    @Test
    fun actionsUseTonalContainerAndPrimaryIcon() {
        assertEquals(BrandPalettes.Light.container.toArgb(), WidgetCabinColors.Forest.actionChip)
        assertEquals(0xFFEEEDFF.toInt(), WidgetCabinColors.Forest.actionChip)
        assertEquals(0xFF5B54E6.toInt(), WidgetCabinColors.Forest.accent)
        assertEquals(BrandPalettes.Dark.container.toArgb(), WidgetCabinColors.ForestDark.actionChip)
        assertEquals(0xFF2E2A66.toInt(), WidgetCabinColors.ForestDark.dayFilled)
        assertEquals(0xFF8F89FF.toInt(), WidgetCabinColors.ForestDark.dayToday)
    }

    @Test
    fun fromScheme_mapsSurfaceAndPrimary() {
        val scheme = forestDarkColorScheme()
        val colors = WidgetCabinColors.fromScheme(scheme, dynamic = true)
        assertTrue(colors.dynamic)
        assertEquals(scheme.surface.toArgb(), colors.bg)
        assertEquals(scheme.primary.toArgb(), colors.accent)
        assertEquals(scheme.onSurface.toArgb(), colors.text)
        assertEquals(scheme.primaryContainer.toArgb(), colors.actionBg)
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
