package com.truckerload.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmailVerificationStoreTest {

    @Before
    fun setUp() {
        SecurePreferences.resetFallbackForTests()
    }

    @Test
    fun beginVerificationMarksPendingAndPeekReturnsCode() {
        val store = EmailVerificationStore(RuntimeEnvironment.getApplication())
        val code = store.beginVerification("Driver@Example.com")

        assertTrue(store.isPending("driver@example.com"))
        assertFalse(store.isVerified("driver@example.com"))
        assertEquals(code, store.peekCode("driver@example.com"))
        assertEquals(6, code.length)
    }

    @Test
    fun verifyCodeClearsPending() {
        val store = EmailVerificationStore(RuntimeEnvironment.getApplication())
        val code = store.beginVerification("a@b.com")

        assertTrue(store.verifyCode("a@b.com", code))
        assertTrue(store.isVerified("a@b.com"))
        assertFalse(store.isPending("a@b.com"))
        assertNull(store.peekCode("a@b.com"))
    }

    @Test
    fun skipForNowKeepsUnverifiedWithoutPending() {
        val store = EmailVerificationStore(RuntimeEnvironment.getApplication())
        store.beginVerification("skip@example.com")
        store.skipForNow("skip@example.com")

        assertFalse(store.isPending("skip@example.com"))
        assertFalse(store.isVerified("skip@example.com"))
    }

    @Test
    fun peekCodeNullWhenNotPending() {
        val store = EmailVerificationStore(RuntimeEnvironment.getApplication())
        assertNull(store.peekCode("nobody@example.com"))
        assertNotNull(store.beginVerification("nobody@example.com"))
    }
}
