package com.truckerload.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration25To26Test {
    @Test
    fun `migration adds media cloud state queue and uniqueness contract`() {
        assertEquals(25, MIGRATION_25_26.startVersion)
        assertEquals(26, MIGRATION_25_26.endVersion)
        val sql = MEDIA_MIGRATION_25_26_SQL.joinToString("\n")
        assertTrue(sql.contains("photos ADD COLUMN cloudMediaId"))
        assertTrue(sql.contains("scans ADD COLUMN cloudSyncStatus"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS media_sync_queue"))
        assertTrue(sql.contains("UNIQUE INDEX IF NOT EXISTS index_media_sync_queue_kind_localId"))
    }
}
