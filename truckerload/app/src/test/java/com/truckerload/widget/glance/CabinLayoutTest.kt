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
    fun fourByThree_isFullWithDayAmountsAndThreeActions() {
        val layout = cabinLayoutFor(CabinSize4x3)
        assertEquals(CabinBucket.FULL, layout.bucket)
        assertTrue(layout.showRpm)
        assertTrue(layout.showWeekRow)
        assertTrue(layout.showDayAmounts)
        assertTrue(layout.showDayLabels)
        assertTrue(layout.showRemaining)
        assertEquals(28.dp, layout.cornerDp)
        assertEquals(3, layout.actionCount)
        assertTrue(layout.actionBtnDp >= 48.dp)
        assertFits(CabinSize4x3, layout)
    }

    @Test
    fun fourByTwo_keepsDayBarsAndLabelsWithoutAmounts() {
        val layout = cabinLayoutFor(CabinSize4x2)
        assertFalse(layout.showDayAmounts)
        assertTrue(layout.showWeekRow)
        assertTrue(layout.showDayLabels)
        assertEquals(3, layout.actionCount)
        assertTrue(layout.actionBtnDp >= 48.dp)
        assertFits(CabinSize4x2, layout)
    }

    @Test
    fun narrowStrip_isMicroWithoutDayBars() {
        val size = DpSize(360.dp, 120.dp)
        val layout = cabinLayoutFor(size)
        assertEquals(CabinBucket.MICRO, layout.bucket)
        assertFalse(layout.showWeekRow)
        assertTrue(layout.headerMetric)
        assertEquals(3, layout.actionCount)
        assertFits(size, layout)
    }

    @Test
    fun square_showsProgressAndCameraThenScanner() {
        val layout = cabinLayoutFor(CabinSize2x2)
        assertEquals(CabinBucket.SQUARE, layout.bucket)
        assertFalse(layout.showWeekRow)
        assertTrue(layout.headerMetric)
        assertEquals(24.dp, layout.cornerDp)
        assertEquals(2, layout.actionCount)
        assertEquals(
            listOf(CabinQuickAction.CAMERA, CabinQuickAction.SCANNER),
            CabinQuickAction.visible(layout.actionCount),
        )
        assertTrue(layout.actionBtnDp >= 48.dp)
        assertFits(CabinSize2x2, layout)
    }

    @Test
    fun oneSlot_keepsCameraOnly() {
        assertEquals(1, CabinQuickAction.countFor(110.dp))
        assertEquals(listOf(CabinQuickAction.CAMERA), CabinQuickAction.visible(1))
    }

    @Test
    fun smallPref_staysCompactEvenWhenTall() {
        val layout = cabinLayoutFor(CabinSize4x4, WidgetSizeMode.SMALL)
        assertEquals(CabinBucket.COMPACT, layout.bucket)
        assertFalse(layout.showDayAmounts)
        assertTrue(layout.showDayLabels)
        assertFits(CabinSize4x4, layout)
    }

    @Test
    fun squeezedWide_hidesDayLabels() {
        listOf(
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 140.dp),
            DpSize(320.dp, 160.dp),
            DpSize(360.dp, 179.dp),
        ).forEach { size ->
            val layout = cabinLayoutFor(size)
            assertTrue("$size progress ${layout.progressBarDp}", layout.progressBarDp <= 10.dp)
            assertFalse(layout.showDayLabels)
            assertFits(size, layout)
        }
    }

    @Test
    fun everyWideHeight_keepsTheActionRowInside() {
        listOf(110, 140, 170, 200, 230, 260, 320, 420, 560).forEach { height ->
            val size = DpSize(320.dp, height.dp)
            val layout = cabinLayoutFor(size)
            assertFits(size, layout)
            assertTrue(layout.actionBtnDp >= 48.dp)
            assertTrue(layout.actionCount >= 1)
        }
    }

    @Test
    fun oversizedWidget_growsInsteadOfLeavingABottomHole() {
        val size = DpSize(340.dp, 460.dp)
        val layout = cabinLayoutFor(size)
        assertEquals(CabinBucket.FULL, layout.bucket)
        assertTrue(layout.actionBtnDp >= 48.dp)
        assertTrue(layout.dayBarDp > 28.dp)
        assertFits(size, layout)
    }

    @Test
    fun mediumHeight_addsPerDayAmountsOnlyWhenTheyFit() {
        val tight = cabinLayoutFor(DpSize(320.dp, 210.dp))
        val roomy = cabinLayoutFor(DpSize(320.dp, 280.dp))
        assertFalse(tight.showDayAmounts)
        assertTrue(tight.showDayLabels)
        assertTrue(roomy.showDayAmounts)
        assertFits(DpSize(320.dp, 210.dp), tight)
        assertFits(DpSize(320.dp, 280.dp), roomy)
    }

    private fun assertFits(size: DpSize, layout: CabinLayout) {
        val wide = size.width >= CabinWideMinWidth
        val fixed = cabinFixedHeight(layout, wide)
        assertTrue(
            "$size bucket=${layout.bucket} fixed=$fixed",
            fixed <= size.height,
        )
    }
}
