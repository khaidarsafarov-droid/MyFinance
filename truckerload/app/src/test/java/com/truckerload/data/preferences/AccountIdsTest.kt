package com.truckerload.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountIdsTest {

    @Test
    fun `fromGoogleSub is stable and namespaced`() {
        val a = AccountIds.fromGoogleSub("abc123sub")
        val b = AccountIds.fromGoogleSub("abc123sub")
        assertEquals(a, b)
        assertTrue(a.startsWith("google_"))
        assertEquals("google_1e4ed2e693f5094efd23d1619187358f", a)
    }

    @Test
    fun `resolve prefers google sub over supabase uuid`() {
        val googleId = AccountIds.fromGoogleSub("sub")
        assertEquals(
            googleId,
            AccountIds.resolveOrNull("uuid-1", "a@b.com", "sub"),
        )
        assertTrue(googleId.startsWith("google_"))
        val emailId = AccountIds.resolveOrNull(null, "driver@example.com", null)
        assertTrue(emailId!!.startsWith("local_"))
        assertNotEquals(googleId, emailId)
        assertEquals("uuid-1", AccountIds.resolveOrNull("uuid-1", "a@b.com", null))
        assertNull(AccountIds.resolveOrNull(null, null, null))
        assertNull(AccountIds.resolveOrNull(null, "  ", "  "))
    }

    @Test
    fun `different google subs isolate accounts`() {
        assertNotEquals(
            AccountIds.fromGoogleSub("sub-a"),
            AccountIds.fromGoogleSub("sub-b"),
        )
    }
}
