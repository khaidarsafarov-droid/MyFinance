package com.truckerload.data.sync.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncModeTest {
    @Test
    fun parse_defaultsToHybrid() {
        assertEquals(SyncMode.HYBRID, SyncMode.parse(null))
        assertEquals(SyncMode.HYBRID, SyncMode.parse(""))
        assertEquals(SyncMode.HYBRID, SyncMode.parse("garbage"))
    }

    @Test
    fun parse_aliases() {
        assertEquals(SyncMode.DEVICE_ONLY, SyncMode.parse("device_only"))
        assertEquals(SyncMode.DEVICE_ONLY, SyncMode.parse("LOCAL"))
        assertEquals(SyncMode.SERVER_PRIMARY, SyncMode.parse("server"))
        assertEquals(SyncMode.HYBRID, SyncMode.parse("HYBRID"))
    }

    @Test
    fun allowsCloudCalls() {
        assertFalse(SyncMode.DEVICE_ONLY.allowsCloudCalls)
        assertTrue(SyncMode.HYBRID.allowsCloudCalls)
        assertTrue(SyncMode.SERVER_PRIMARY.allowsCloudCalls)
        assertTrue(SyncMode.SERVER_PRIMARY.prefersCloudRefresh)
        assertFalse(SyncMode.HYBRID.prefersCloudRefresh)
    }
}
