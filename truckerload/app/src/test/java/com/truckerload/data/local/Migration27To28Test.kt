package com.truckerload.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration27To28Test {
    @Test
    fun migrationAddsServiceNameColumn() {
        assertEquals(27, MIGRATION_27_28.startVersion)
        assertEquals(28, MIGRATION_27_28.endVersion)
    }
}
