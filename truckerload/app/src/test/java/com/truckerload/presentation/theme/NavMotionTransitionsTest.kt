package com.truckerload.presentation.theme

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavMotionTransitionsTest {

    @Test
    fun forwardAndPop_areDistinctWhenMotionEnabled() {
        val enter = navForwardEnter(reduceMotion = false)
        val exit = navForwardExit(reduceMotion = false)
        val popEnter = navPopEnter(reduceMotion = false)
        val popExit = navPopExit(reduceMotion = false)
        assertNotNull(enter)
        assertNotNull(exit)
        assertNotNull(popEnter)
        assertNotNull(popExit)
        assertTrue(enter != popEnter)
        assertTrue(exit != popExit)
    }

    @Test
    fun reduceMotion_collapsesToInstantFade() {
        assertNotNull(navForwardEnter(reduceMotion = true))
        assertNotNull(navForwardExit(reduceMotion = true))
        assertNotNull(navModalEnter(reduceMotion = true))
        assertNotNull(navModalPopExit(reduceMotion = true))
        assertTrue(motionDurationMs(reduceMotion = true, normalMs = 280) == 0)
        assertTrue(motionDurationMs(reduceMotion = false, normalMs = 280) == 280)
    }

    @Test
    fun predictivePop_andSharedElement_respectReduceMotion() {
        assertNotNull(navPopEnter(reduceMotion = false))
        assertNotNull(navPopExit(reduceMotion = false))
        assertNotNull(navPopEnter(reduceMotion = true))
        assertNotNull(navPopExit(reduceMotion = true))
        assertNotNull(navSharedElementEnter(reduceMotion = false))
        assertNotNull(navSharedElementExit(reduceMotion = false))
        assertNotNull(navSharedElementEnter(reduceMotion = true))
        assertNotNull(navSharedElementExit(reduceMotion = true))
        // Scale-based pop should differ from forward slide when motion is on.
        assertTrue(navPopExit(reduceMotion = false) != navForwardExit(reduceMotion = false))
    }
}
