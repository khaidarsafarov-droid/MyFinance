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
}
