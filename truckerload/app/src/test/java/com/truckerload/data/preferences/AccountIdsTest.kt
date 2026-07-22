package com.truckerload.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountIdsTest {

    @Test
    fun resolveOrNull_prefersSupabaseId() {
        assertEquals(
            "uuid-abc",
            AccountIds.resolveOrNull("uuid-abc", "a@example.com"),
        )
    }

    @Test
    fun resolveOrNull_fallsBackToEmailHash() {
        val id = AccountIds.resolveOrNull(null, "Driver@Example.com")
        assertTrue(id!!.startsWith("local_"))
        assertEquals(id, AccountIds.fromEmail("driver@example.com"))
    }

    @Test
    fun resolveOrNull_blankEmailWithoutSupabase_returnsNull() {
        assertNull(AccountIds.resolveOrNull(null, "  "))
        assertNull(AccountIds.resolveOrNull("", null))
    }

    @Test
    fun differentEmails_getDifferentIds() {
        assertNotEquals(
            AccountIds.fromEmail("a@test.com"),
            AccountIds.fromEmail("b@test.com"),
        )
    }

    @Test
    fun sanitizeFilePart_stripsUnsafeChars() {
        assertEquals(
            "user_12_ab_",
            AccountIds.sanitizeFilePart(" user/12@ab! "),
        )
    }
}

