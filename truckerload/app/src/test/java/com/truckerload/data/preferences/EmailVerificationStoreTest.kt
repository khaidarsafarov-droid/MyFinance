package com.truckerload.data.preferences

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @After
    fun tearDown() {
        SecurePreferences.resetFallbackForTests()
    }

    @Test
    fun beginVerificationReusesPendingCodeAndPeekShowsIt() {
        val store = EmailVerificationStore(RuntimeEnvironment.getApplication())
        val first = store.beginVerification("Driver@Example.com")
        val second = store.beginVerification("driver@example.com")

        assertEquals(first, second)
        assertTrue(store.isPending("driver@example.com"))
        assertEquals(first, store.peekCode("Driver@Example.com"))
        assertTrue(store.verifyCode("driver@example.com", first))
        assertTrue(store.isVerified("driver@example.com"))
        assertFalse(store.isPending("driver@example.com"))
    }

    @Test
    fun skipForNowClearsPendingButKeepsUnverified() {
        val store = EmailVerificationStore(RuntimeEnvironment.getApplication())
        val code = store.beginVerification("a@b.co")
        assertNotNull(code)
        store.skipForNow("a@b.co")
        assertFalse(store.isPending("a@b.co"))
        assertFalse(store.isVerified("a@b.co"))
    }
}
