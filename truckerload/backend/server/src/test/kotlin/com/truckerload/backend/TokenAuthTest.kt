package com.truckerload.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenAuthTest {
    @Test
    fun `google account id matches Android AccountIds fromGoogleSub`() {
        val id = TokenAuth.googleAccountId("abc123sub")
        assertTrue(id.startsWith("google_"))
        assertEquals(39, id.length)
        assertEquals(id, TokenAuth.googleAccountId("abc123sub"))
        // Same SHA-256 prefix as com.truckerload.data.preferences.AccountIds.fromGoogleSub
        assertEquals("google_1e4ed2e693f5094efd23d1619187358f", TokenAuth.googleAccountId("abc123sub"))
    }
}
