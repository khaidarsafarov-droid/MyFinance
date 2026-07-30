package com.truckerload.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration25To26Test {
    @Test
    fun `migration object and column contract`() {
        assertEquals(25, MIGRATION_25_26.startVersion)
        assertEquals(26, MIGRATION_25_26.endVersion)
        assertTrue(MEDIA_MIGRATION_25_26_COLUMNS.contains("photos.cloudMediaId"))
        assertTrue(MEDIA_MIGRATION_25_26_COLUMNS.contains("scans.cloudSyncStatus"))
        assertTrue(MEDIA_MIGRATION_25_26_COLUMNS.any { it.startsWith("photos.") })
        assertTrue(MEDIA_MIGRATION_25_26_COLUMNS.any { it.startsWith("scans.") })
    }
}
