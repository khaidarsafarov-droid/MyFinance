package com.truckerload.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollableContentTest {

    @Test
    fun dialogBodyMaxHeightDp_usesFractionWithFloor() {
        assertEquals(240, dialogBodyMaxHeightDp(400))
        assertEquals(440, dialogBodyMaxHeightDp(800))
        assertEquals(704, dialogBodyMaxHeightDp(1280))
        assertTrue(dialogBodyMaxHeightDp(2000) < 2000)
    }
}
