package com.truckerload.presentation.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowSizeClassTest {

    @Test
    fun compact_below600() {
        assertEquals(WindowSizeClass.COMPACT, windowSizeClassForWidth(320))
        assertEquals(WindowSizeClass.COMPACT, windowSizeClassForWidth(599))
    }

    @Test
    fun medium_portraitTabletRange() {
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(600))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(768))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(839))
    }

    @Test
    fun expanded_landscapeTabletRange() {
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassForWidth(840))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassForWidth(1024))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassForWidth(1366))
    }

    @Test
    fun breakpoints_matchDocumentedTailwindAnalogs() {
        assertEquals(600, WindowBreakpoints.MEDIUM_MIN)
        assertEquals(840, WindowBreakpoints.EXPANDED_MIN)
        assertEquals(1280, WindowBreakpoints.XL_MIN)
    }
}
