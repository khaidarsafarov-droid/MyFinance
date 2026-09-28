package com.truckerload.widget.glance

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.truckerload.widget.WidgetSizeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinLayoutTest {

    @Test
    fun fullHeight_matchesMockupTokens() {
        val layout = cabinLayoutFor(CabinSize4x4)
        assertEquals(16.dp, layout.paddingH)
        assertEquals(12.dp, layout.progressBarDp)
        assertTrue(layout.progressBarHeadroomDp >= 14.dp)
        assertEquals(28.dp, layout.dayChipDp)
        assertEquals(42.dp, layout.actionBtnDp)
        assertTrue(layout.showDayCaptions)
    }

    @Test
    fun defaultWide_keepsDayCaptions() {
        val layout = cabinLayoutFor(CabinSize4x3)
        assertTrue(layout.showDayCaptions)
        assertTrue(layout.progressBarDp >= 10.dp)
    }

    @Test
    fun compactWide_hidesDayCaptions() {
        val layout = cabinLayoutFor(CabinSize4x2)
        assertFalse(layout.showDayCaptions)
    }

    @Test
    fun squeezedFourByTwo_usesCompactProgressBar() {
        listOf(
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 140.dp),
            DpSize(320.dp, 160.dp),
            DpSize(360.dp, 179.dp),
        ).forEach { size ->
            val layout = cabinLayoutFor(size)
            assertTrue("$size should use compact progress bar", layout.progressBarDp <= 10.dp)
            assertEquals(CabinBucket.COMPACT, cabinBucket(size))
        }
    }

    @Test
    fun smallPref_staysCompactEvenWhenTall() {
        val layout = cabinLayoutFor(CabinSize4x4, WidgetSizeMode.SMALL)
        assertFalse(layout.showDayCaptions)
        assertEquals(CabinBucket.COMPACT, cabinBucket(CabinSize4x4, WidgetSizeMode.SMALL))
    }

    @Test
    fun square_hidesDayRowChrome() {
        val layout = cabinLayoutFor(CabinSize2x2)
        assertFalse(layout.showDayCaptions)
    }
}
