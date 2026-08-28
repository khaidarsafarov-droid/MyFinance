package com.truckerload.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppConfigGoogleAuthTest {
    @Test
    fun fromEnvironment_doesNotHardcodeGoogleClientId() {
        val config = AppConfig.fromEnvironment(
            mapOf(
                "APP_ENV" to "test",
                "DATABASE_URL" to "jdbc:postgresql://localhost/test",
                "SUPABASE_JWT_SECRET" to "test-jwt-secret-not-for-production",
                "SUPABASE_JWT_ISSUER" to "https://test.supabase.co/auth/v1",
                "TELEGRAM_WEBHOOK_SECRET" to "telegram-test-secret",
                "LOCAL_STORAGE_SIGNING_SECRET" to "local-test-signing-secret",
                "PUBLIC_BASE_URL" to "http://localhost:8080",
            ),
        )
        assertEquals("", config.googleWebClientId)
    }

    @Test
    fun fromEnvironment_readsGoogleClientIdFromEnv() {
        val expected = "my-client.apps.googleusercontent.com"
        val config = AppConfig.fromEnvironment(
            mapOf(
                "APP_ENV" to "test",
                "DATABASE_URL" to "jdbc:postgresql://localhost/test",
                "SUPABASE_JWT_SECRET" to "test-jwt-secret-not-for-production",
                "SUPABASE_JWT_ISSUER" to "https://test.supabase.co/auth/v1",
                "TELEGRAM_WEBHOOK_SECRET" to "telegram-test-secret",
                "LOCAL_STORAGE_SIGNING_SECRET" to "local-test-signing-secret",
                "PUBLIC_BASE_URL" to "http://localhost:8080",
                "GOOGLE_WEB_CLIENT_ID" to expected,
            ),
        )
        assertEquals(expected, config.googleWebClientId)
    }

    @Test
    fun testFactory_providesExplicitGoogleClientId() {
        assertTrue(AppConfig.test().googleWebClientId.isNotBlank())
    }
}
