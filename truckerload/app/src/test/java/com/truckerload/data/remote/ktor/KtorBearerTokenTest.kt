package com.truckerload.data.remote.ktor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KtorBearerTokenTest {

    @Test
    fun accessToken_isUsedWhenPresent() {
        assertEquals("supabase.jwt", KtorBearerToken.select("supabase.jwt"))
    }

    @Test
    fun blankAccessToken_returnsNull() {
        assertNull(KtorBearerToken.select("  "))
        assertNull(KtorBearerToken.select(null))
    }
}
