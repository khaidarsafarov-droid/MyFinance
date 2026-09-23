package com.truckerload.widget

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WidgetTodayPointerTest {

    @Test
    fun pointer_tipAndBaseAreBrandPurple_cornersStayClear() {
        val color = WidgetCabinPalette.RING
        val bmp = WidgetWeekDaysBitmap.createTodayPointer(40, 24, color)

        assertBrand(bmp.getPixel(20, 16), color)
        assertBrand(bmp.getPixel(20, 3), color)
        assertTrue(Color.alpha(bmp.getPixel(1, 1)) < 40)
        assertTrue(Color.alpha(bmp.getPixel(38, 1)) < 40)
    }

    @Test
    fun pointer_usesAppPurple_notDarkBrandText() {
        assertEquals(0xFF5B54E6.toInt(), WidgetCabinPalette.RING)
        assertEquals(WidgetCabinPalette.RING, WidgetCabinColors.ForestDark.ring)
        assertEquals(WidgetCabinPalette.RING, WidgetCabinColors.ForestLight.ring)
    }

    private fun assertBrand(px: Int, color: Int) {
        assertTrue("expected opaque brand pixel, alpha=${Color.alpha(px)}", Color.alpha(px) > 200)
        assertEquals(Color.red(color), Color.red(px))
        assertEquals(Color.green(color), Color.green(px))
        assertEquals(Color.blue(color), Color.blue(px))
    }
}
