package com.truckerload.data.remote.ktor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KtorBearerTokenTest {

    @Test
    fun voiceTokenPath_prefersGoogleIdToken() {
        assertEquals(
            "ya29.google-id",
            KtorBearerToken.select(
                encodedPath = "/v1/voice/token",
                googleIdToken = "ya29.google-id",
                accessToken = "supabase.jwt",
            ),
        )
    }

    @Test
    fun voiceTokenPath_fallsBackToAccessToken() {
        assertEquals(
            "supabase.jwt",
            KtorBearerToken.select(
                encodedPath = "/v1/voice/token",
                googleIdToken = null,
                accessToken = "supabase.jwt",
            ),
        )
    }

    @Test
    fun snapshotPath_keepsSupabaseAccessToken() {
        assertEquals(
            "supabase.jwt",
            KtorBearerToken.select(
                encodedPath = "/v1/snapshots",
                googleIdToken = "ya29.google-id",
                accessToken = "supabase.jwt",
            ),
        )
    }

    @Test
    fun blankGoogleToken_doesNotOverrideAccessToken() {
        assertEquals(
            "supabase.jwt",
            KtorBearerToken.select(
                encodedPath = "/v1/voice/token",
                googleIdToken = "  ",
                accessToken = "supabase.jwt",
            ),
        )
    }

    @Test
    fun missingTokens_returnsNull() {
        assertNull(
            KtorBearerToken.select(
                encodedPath = "/v1/voice/token",
                googleIdToken = null,
                accessToken = null,
            ),
        )
    }
}
