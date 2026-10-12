package com.truckerload.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun `email ids are namespaced apart from google ids`() {
        val googleId = AccountIds.fromGoogleSub("sub")
        val emailId = AccountIds.fromEmail("driver@example.com")
        assertTrue(emailId.startsWith("local_"))
        assertNotEquals(googleId, emailId)
    }

    @Test
    fun `different google subs isolate accounts`() {
        assertNotEquals(
            AccountIds.fromGoogleSub("sub-a"),
            AccountIds.fromGoogleSub("sub-b"),
        )
    }
}
