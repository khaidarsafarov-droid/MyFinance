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
        assertEquals(10.dp, layout.progressBarDp)
        assertEquals(28.dp, layout.dayChipDp)
        assertEquals(40.dp, layout.actionBtnDp)
        assertTrue(layout.showDayCaptions)
        assertTrue(layout.showQuickActionsTitle)
        assertTrue(layout.showActionLabels)
        assertTrue(layout.showQuickActions)
    }

    @Test
    fun defaultWide_keepsCaptionsAndActions() {
        val layout = cabinLayoutFor(CabinSize4x3)
        assertTrue(layout.showDayCaptions)
        assertTrue(layout.showQuickActionsTitle)
        assertTrue(layout.showActionLabels)
        assertTrue(layout.progressBarDp >= 8.dp)
        assertTrue(layout.showQuickActions)
    }

    @Test
    fun compactWide_hidesCaptionsAndActionLabels() {
        val layout = cabinLayoutFor(CabinSize4x2)
        assertFalse(layout.showDayCaptions)
        assertFalse(layout.showQuickActionsTitle)
        assertFalse(layout.showActionLabels)
        assertTrue(layout.showQuickActions)
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
            assertTrue("$size should use compact progress bar", layout.progressBarDp <= 8.dp)
            assertEquals(CabinBucket.COMPACT, cabinBucket(size))
        }
    }

    @Test
    fun smallPref_staysCompactEvenWhenTall() {
        val layout = cabinLayoutFor(CabinSize4x4, WidgetSizeMode.SMALL)
        assertFalse(layout.showQuickActionsTitle)
        assertEquals(CabinBucket.COMPACT, cabinBucket(CabinSize4x4, WidgetSizeMode.SMALL))
    }

    @Test
    fun square_hidesDayRowChrome() {
        val layout = cabinLayoutFor(CabinSize2x2)
        assertFalse(layout.showDayCaptions)
        assertFalse(layout.showQuickActionsTitle)
        assertFalse(layout.showActionLabels)
        assertTrue(layout.showQuickActions)
    }
}
