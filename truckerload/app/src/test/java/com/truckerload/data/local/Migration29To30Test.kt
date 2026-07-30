package com.truckerload.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration29To30Test {
    @Test
    fun migrationAddsUniqueStopsIndex() {
        assertEquals(29, MIGRATION_29_30.startVersion)
        assertEquals(30, MIGRATION_29_30.endVersion)
    }
}
