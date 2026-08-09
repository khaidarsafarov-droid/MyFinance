package com.truckerload.presentation.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowSizeClassTest {

    @Test
    fun windowSizeClassForWidth_matchesMaterialBuckets() {
        assertEquals(WindowSizeClass.COMPACT, windowSizeClassForWidth(359))
        assertEquals(WindowSizeClass.COMPACT, windowSizeClassForWidth(599))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(600))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(768))
        assertEquals(WindowSizeClass.MEDIUM, windowSizeClassForWidth(839))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassForWidth(840))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassForWidth(1024))
        assertEquals(WindowSizeClass.EXPANDED, windowSizeClassForWidth(1366))
    }

    @Test
    fun isTabletWidth_trueFromMediumUp() {
        assertFalse(isTabletWidth(411))
        assertFalse(isTabletWidth(599))
        assertTrue(isTabletWidth(600))
        assertTrue(isTabletWidth(1024))
    }

    @Test
    fun useTwoPaneForWidth_onlyExpanded() {
        assertFalse(useTwoPaneForWidth(768))
        assertFalse(useTwoPaneForWidth(839))
        assertTrue(useTwoPaneForWidth(840))
        assertTrue(useTwoPaneForWidth(1280))
    }

    @Test
    fun adaptiveGridColumns_portraitTwo_landscapeThree() {
        assertEquals(1, adaptiveGridColumnsForWidth(400, compact = 1, medium = 2, expanded = 3))
        assertEquals(2, adaptiveGridColumnsForWidth(768, compact = 1, medium = 2, expanded = 3))
        assertEquals(3, adaptiveGridColumnsForWidth(1024, compact = 1, medium = 2, expanded = 3))
        // Analytics-style metrics stay 2-up on phones.
        assertEquals(2, adaptiveGridColumnsForWidth(390, compact = 2, medium = 2, expanded = 3))
    }
}
