package com.truckerload.widget.glance

import androidx.compose.ui.unit.dp
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
    }

    @Test
    fun defaultWide_keepsCaptionsAndActions() {
        val layout = cabinLayoutFor(CabinSize4x3)
        assertTrue(layout.showDayCaptions)
        assertTrue(layout.showDivider)
        assertTrue(layout.showActionLabels)
        assertTrue(layout.ringDp < 92.dp)
    }

    @Test
    fun compactWide_hidesCaptionsAndDivider() {
        val layout = cabinLayoutFor(CabinSize4x2)
        assertFalse(layout.showDayCaptions)
        assertFalse(layout.showDivider)
        assertTrue(layout.showActionLabels)
    }

    @Test
    fun square_hidesDayRowChrome() {
        val layout = cabinLayoutFor(CabinSize2x2)
        assertFalse(layout.showDayCaptions)
        assertFalse(layout.showDivider)
        assertFalse(layout.showActionLabels)
    }
}
