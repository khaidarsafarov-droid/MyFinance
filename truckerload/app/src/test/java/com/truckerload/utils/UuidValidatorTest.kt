package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UuidValidatorTest {

    @Test
    fun acceptsStandardUuid() {
        val id = "550e8400-e29b-41d4-a716-446655440000"
        assertTrue(UuidValidator.isUuid(id))
        assertEquals(id, UuidValidator.sanitizeFilterIdOrNull(id))
    }

    @Test
    fun acceptsLocalDevAccountId() {
        assertEquals("local_dev", UuidValidator.sanitizeFilterIdOrNull("local_dev"))
    }

    @Test
    fun rejectsPostgrestMetacharacters() {
        assertNull(UuidValidator.sanitizeFilterIdOrNull("abc&or=1"))
        assertNull(UuidValidator.sanitizeFilterIdOrNull("x=eq.y"))
        assertNull(UuidValidator.sanitizeFilterIdOrNull("a,b"))
        assertFalse(UuidValidator.isUuid("not-a-uuid"))
    }
}
