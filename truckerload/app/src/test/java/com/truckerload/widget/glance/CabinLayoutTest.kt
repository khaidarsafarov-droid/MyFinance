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
        assertEquals(20.dp, layout.paddingH)
        assertEquals(20.dp, layout.paddingV)
        assertEquals(92.dp, layout.ringDp)
        assertEquals(30.dp, layout.dayChipDp)
        assertEquals(44.dp, layout.actionBtnDp)
        assertEquals(20.dp, layout.actionIconDp)
        assertTrue(layout.showDayCaptions)
        assertTrue(layout.showDivider)
        assertTrue(layout.showActionLabels)
        assertTrue(layout.showRing)
    }

    @Test
    fun defaultWide_keepsCaptionsAndActions() {
        val layout = cabinLayoutFor(CabinSize4x3)
        assertTrue(layout.showDayCaptions)
        assertTrue(layout.showDivider)
        assertTrue(layout.showActionLabels)
        assertTrue(layout.ringDp < 92.dp)
        assertTrue(layout.showRing)
    }

    @Test
    fun compactWide_hidesCaptionsAndDivider() {
        val layout = cabinLayoutFor(CabinSize4x2)
        assertFalse(layout.showDayCaptions)
        assertFalse(layout.showDivider)
        assertFalse(layout.showActionLabels)
        assertFalse(layout.showRing)
    }

    @Test
    fun squeezedFourByTwo_hidesRing() {
        listOf(
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 140.dp),
            DpSize(320.dp, 160.dp),
            DpSize(360.dp, 179.dp),
        ).forEach { size ->
            val layout = cabinLayoutFor(size)
            assertFalse("$size should hide the ring", layout.showRing)
            assertEquals(CabinBucket.COMPACT, cabinBucket(size))
        }
    }

    @Test
    fun smallPref_hidesRingEvenWhenTall() {
        val layout = cabinLayoutFor(CabinSize4x4, WidgetSizeMode.SMALL)
        assertFalse(layout.showRing)
        assertEquals(CabinBucket.COMPACT, cabinBucket(CabinSize4x4, WidgetSizeMode.SMALL))
    }

    @Test
    fun square_hidesDayRowChrome() {
        val layout = cabinLayoutFor(CabinSize2x2)
        assertFalse(layout.showDayCaptions)
        assertFalse(layout.showDivider)
        assertFalse(layout.showActionLabels)
        assertFalse(layout.showRing)
    }
}
