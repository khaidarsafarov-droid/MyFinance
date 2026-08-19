package com.truckerload.domain.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthProviderTest {
    @Test
    fun `apple remains a reserved provider name for later publication`() {
        assertEquals(
            setOf("EMAIL", "GOOGLE", "APPLE"),
            AuthProvider.entries.map { it.name }.toSet(),
        )
    }
}
