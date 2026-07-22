package com.truckerload.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration22To23Test {
    @Test
    fun migrationObjectExists() {
        assertEquals(22, MIGRATION_22_23.startVersion)
        assertEquals(23, MIGRATION_22_23.endVersion)
    }
}
