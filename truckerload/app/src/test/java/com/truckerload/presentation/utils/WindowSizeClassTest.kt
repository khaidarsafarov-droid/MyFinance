package com.truckerload.presentation.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowSizeClassTest {

    @Test
    fun windowSizeClassForWidth_buckets() {
        assertEquals(WindowSizeClass.COMPACT, windowSizeClassForWidth(411))
        assertEquals(WindowSizeClass.COMPACT, windowSizeClassForWidth(599))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(600))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(768))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(839))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassForWidth(840))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassForWidth(1280))
    }

    @Test
    fun isTabletClassWidth() {
        assertFalse(isTabletClassWidth(411))
        assertTrue(isTabletClassWidth(600))
        assertTrue(isTabletClassWidth(1024))
    }
}
