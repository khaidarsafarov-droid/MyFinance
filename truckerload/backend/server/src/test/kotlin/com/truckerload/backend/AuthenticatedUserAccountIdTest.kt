package com.truckerload.backend

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthenticatedUserAccountIdTest {
    @Test
    fun acceptsUuidAndGoogleVoiceIdentity() {
        val googleId = TokenAuth.googleAccountId("abc123sub")
        val userId = UUID.nameUUIDFromBytes(googleId.toByteArray(Charsets.UTF_8))
        val user = AuthenticatedUser(userId, "driver@example.com", voiceIdentity = googleId)

        assertTrue(user.acceptsAccountId(userId.toString()))
        assertTrue(user.acceptsAccountId(googleId))
        assertTrue(user.acceptsAccountId("  $googleId  "))
        assertFalse(user.acceptsAccountId("google_other"))
        assertFalse(user.acceptsAccountId(UUID.randomUUID().toString()))
    }
}
