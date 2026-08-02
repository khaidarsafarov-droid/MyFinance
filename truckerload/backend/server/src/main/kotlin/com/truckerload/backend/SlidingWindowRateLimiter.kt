package com.truckerload.backend

import java.util.concurrent.ConcurrentHashMap

/**
 * Process-local sliding-window rate limiter for abuse protection.
 * Not a substitute for edge/CDN limits, but blocks obvious floods in-app.
 */
class SlidingWindowRateLimiter(
    private val maxRequests: Int,
    private val windowMs: Long,
) {
    private val hits = ConcurrentHashMap<String, ArrayDeque<Long>>()

    /** @return true when the request is allowed. */
    fun allow(key: String): Boolean {
        val now = System.currentTimeMillis()
        val queue = hits.computeIfAbsent(key) { ArrayDeque() }
        synchronized(queue) {
            while (queue.isNotEmpty() && now - queue.first() > windowMs) {
                queue.removeFirst()
            }
            if (queue.size >= maxRequests) return false
            queue.addLast(now)
            return true
        }
    }
}
