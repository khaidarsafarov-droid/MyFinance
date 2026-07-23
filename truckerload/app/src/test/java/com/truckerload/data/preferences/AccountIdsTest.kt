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
    }

    @Test
    fun `resolve prefers supabase then google then email`() {
        assertEquals(
            "uuid-1",
            AccountIds.resolveOrNull("uuid-1", "a@b.com", "sub"),
        )
        val googleId = AccountIds.resolveOrNull(null, "a@b.com", "google-sub-xyz")
        assertTrue(googleId!!.startsWith("google_"))
        val emailId = AccountIds.resolveOrNull(null, "driver@example.com", null)
        assertTrue(emailId!!.startsWith("local_"))
        assertNotEquals(googleId, emailId)
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
