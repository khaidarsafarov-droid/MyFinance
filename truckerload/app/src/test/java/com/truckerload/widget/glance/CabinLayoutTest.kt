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
        assertEquals(CabinBucket.FULL, layout.bucket)
        assertEquals(16.dp, layout.paddingH)
        assertEquals(12.dp, layout.progressBarDp)
        assertTrue(layout.progressBarHeadroomDp >= 14.dp)
        assertEquals(28.dp, layout.dayChipDp)
        assertEquals(42.dp, layout.actionBtnDp)
        assertTrue(layout.showWeekRow)
        assertTrue(layout.showDayCaptions)
        assertFits(CabinSize4x4, layout)
    }

    @Test
    fun threeRowMinimum_dropsCaptionsSoActionsFit() {
        val layout = cabinLayoutFor(CabinSize4x3)
        assertFalse(layout.showDayCaptions)
        assertTrue(layout.showWeekRow)
        assertFits(CabinSize4x3, layout)
    }

    @Test
    fun compactWide_hidesDayCaptions() {
        val layout = cabinLayoutFor(CabinSize4x2)
        assertFalse(layout.showDayCaptions)
        assertFits(CabinSize4x2, layout)
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
            assertTrue("$size progress ${layout.progressBarDp}", layout.progressBarDp <= 10.dp)
            assertFalse(layout.showDayCaptions)
            assertFits(size, layout)
        }
    }

    @Test
    fun smallPref_staysCompactEvenWhenTall() {
        val layout = cabinLayoutFor(CabinSize4x4, WidgetSizeMode.SMALL)
        assertFalse(layout.showDayCaptions)
        assertEquals(CabinBucket.COMPACT, cabinBucket(CabinSize4x4, WidgetSizeMode.SMALL))
        assertFits(CabinSize4x4, layout)
    }

    @Test
    fun square_hidesDayRowChrome() {
        val layout = cabinLayoutFor(CabinSize2x2)
        assertFalse(layout.showDayCaptions)
        assertFalse(layout.showWeekRow)
        assertFits(CabinSize2x2, layout)
    }

    @Test
    fun everyWideHeight_keepsTheActionRowInside() {
        listOf(110, 140, 170, 200, 230, 260, 320, 420, 560).forEach { height ->
            val size = DpSize(320.dp, height.dp)
            val layout = cabinLayoutFor(size)
            assertFits(size, layout)
            assertTrue(layout.actionBtnDp >= 24.dp)
        }
    }

    @Test
    fun oversizedWidget_growsInsteadOfLeavingABottomHole() {
        val layout = cabinLayoutFor(DpSize(340.dp, 460.dp))
        assertEquals(CabinBucket.FULL, layout.bucket)
        assertTrue(layout.actionBtnDp > 42.dp)
        assertTrue(layout.dayChipDp > 28.dp)
        assertFits(DpSize(340.dp, 460.dp), layout)
    }

    @Test
    fun mediumHeight_showsCaptionsOnlyWhenTheyFit() {
        val tight = cabinLayoutFor(DpSize(320.dp, 210.dp))
        val roomy = cabinLayoutFor(DpSize(320.dp, 280.dp))
        assertFalse(tight.showDayCaptions)
        assertTrue(roomy.showDayCaptions)
        assertFits(DpSize(320.dp, 210.dp), tight)
        assertFits(DpSize(320.dp, 280.dp), roomy)
    }

    private fun assertFits(size: DpSize, layout: CabinLayout) {
        val wide = size.width >= CabinSize4x2.width
        val fixed = cabinFixedHeight(layout, wide)
        assertTrue(
            "$size bucket=${layout.bucket} fixed=$fixed",
            fixed <= size.height,
        )
    }
}
