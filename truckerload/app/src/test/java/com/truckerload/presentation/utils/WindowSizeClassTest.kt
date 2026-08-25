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

    @Test
    fun useTwoPaneForWidth() {
        assertFalse(useTwoPaneForWidth(768))
        assertTrue(useTwoPaneForWidth(1024))
        assertFalse(useTwoPaneForWidth(widthDp = 914, smallestWidthDp = 411))
        assertTrue(useTwoPaneForWidth(widthDp = 1024, smallestWidthDp = 800))
        assertFalse(useTwoPaneForWidth(widthDp = 800, smallestWidthDp = 800))
    }

    @Test
    fun tabletChrome_usesSmallestWidthNotPhoneLandscape() {
        assertFalse(isTabletDevice(411))
        assertTrue(isTabletDevice(600))
        assertTrue(isTabletDevice(800))

        assertFalse(useTabletChrome(smallestWidthDp = 411, widthDp = 914))
        assertTrue(useTabletChrome(smallestWidthDp = 800, widthDp = 800))
        assertTrue(useTabletChrome(smallestWidthDp = 800, widthDp = 1280))
        assertFalse(useTabletChrome(smallestWidthDp = 800, widthDp = 400))
    }

    @Test
    fun wideSidebar_onlyOnLargeTabletWindows() {
        assertFalse(useWideTabletSidebar(smallestWidthDp = 800, widthDp = 800))
        assertFalse(useWideTabletSidebar(smallestWidthDp = 600, widthDp = 1023))
        assertTrue(useWideTabletSidebar(smallestWidthDp = 800, widthDp = 1024))
        assertTrue(useWideTabletSidebar(smallestWidthDp = 800, widthDp = 1280))
        assertFalse(useWideTabletSidebar(smallestWidthDp = 411, widthDp = 1280))
    }

    @Test
    fun adaptiveGridColumnsForWidth_defaults() {
        assertEquals(1, adaptiveGridColumnsForWidth(411))
        assertEquals(2, adaptiveGridColumnsForWidth(768))
        assertEquals(3, adaptiveGridColumnsForWidth(1024))
    }
}
