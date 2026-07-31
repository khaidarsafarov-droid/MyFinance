package com.truckerload.data.sync.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictResolverTest {
    private val resolver = SyncConflictResolver()

    @Test
    fun remoteWins_onNewerTimestamp() {
        assertTrue(resolver.remoteWins(localUpdatedAt = 10L, remoteUpdatedAt = 20L))
        assertFalse(resolver.remoteWins(localUpdatedAt = 20L, remoteUpdatedAt = 20L))
        assertTrue(resolver.remoteWins(localUpdatedAt = null, remoteUpdatedAt = 1L))
    }

    @Test
    fun mergeById_keepsNewer() {
        data class Row(val id: String, val updatedAt: Long)
        val local = mapOf("a" to Row("a", 10), "b" to Row("b", 50))
        val remote = mapOf("a" to Row("a", 30), "c" to Row("c", 1))
        val merged = resolver.mergeById(local, remote) { it.updatedAt }
        assertEquals(30L, merged.getValue("a").updatedAt)
        assertEquals(50L, merged.getValue("b").updatedAt)
        assertEquals(1L, merged.getValue("c").updatedAt)
    }

    @Test
    fun hydrationAndIncrementalGates() {
        assertTrue(resolver.needsFullHydration(0L, 0, 3))
        assertFalse(resolver.needsFullHydration(1L, 0, 3))
        assertTrue(resolver.shouldPullIncremental(10L, 20L))
        assertFalse(resolver.shouldPullIncremental(0L, 20L))
    }
}
