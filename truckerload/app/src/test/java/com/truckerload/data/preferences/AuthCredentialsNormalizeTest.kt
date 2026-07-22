package com.truckerload.data.preferences

import com.truckerload.data.remote.SupabaseAuthService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCredentialsNormalizeTest {

    @Test
    fun normalizeEmail_trimsAndLowercases() {
        assertEquals("a@test.com", AuthCredentialsStore.normalizeEmail("  A@Test.COM "))
    }

    @Test
    fun isEmailSendRateLimited_detectsSupabaseCode() {
        val err = SupabaseAuthService.AuthApiException(
            "rate",
            errorCode = "over_email_send_rate_limit",
            httpCode = 429,
        )
        assertTrue(SupabaseAuthService.isEmailSendRateLimited(err))
        assertFalse(SupabaseAuthService.isEmailSendRateLimited(Exception("invalid login")))
    }
}
