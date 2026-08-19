package com.truckerload.backend

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlidingWindowRateLimiterTest {

    @Test
    fun allowsUpToMaxThenRejects() {
        val limiter = SlidingWindowRateLimiter(maxRequests = 3, windowMs = 60_000)
        assertTrue(limiter.allow("k"))
        assertTrue(limiter.allow("k"))
        assertTrue(limiter.allow("k"))
        assertFalse(limiter.allow("k"))
    }

    @Test
    fun keysAreIndependent() {
        val limiter = SlidingWindowRateLimiter(maxRequests = 1, windowMs = 60_000)
        assertTrue(limiter.allow("a"))
        assertTrue(limiter.allow("b"))
        assertFalse(limiter.allow("a"))
    }
}
