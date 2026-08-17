package com.truckerload.data.preferences

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramOnboardingStoreTest {

    @Before
    fun setUp() {
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
    }

    @After
    fun tearDown() {
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
    }

    @Test
    fun completedFlagSurvivesRereadAndHidesPrompt() {
        val context = RuntimeEnvironment.getApplication()
        AuthStore(context).login(
            userId = "user-onboard-1",
            email = "onboard@example.com",
            rememberMe = true,
            provider = AuthProvider.EMAIL,
        )
        val store = TelegramOnboardingStore(context, "user-onboard-1")
        assertFalse(store.isCompleted())

        store.markCompleted()

        assertTrue(store.isCompleted())
        assertFalse(store.shouldPrompt(context))
        assertTrue(TelegramOnboardingStore(context, "user-onboard-1").isCompleted())
        assertFalse(TelegramOnboardingStore(context, "user-onboard-1").shouldPrompt(context))
    }

    @Test
    fun skipIsPerAccount() {
        val context = RuntimeEnvironment.getApplication()
        val first = TelegramOnboardingStore(context, "user-a")
        first.markCompleted()
        val second = TelegramOnboardingStore(context, "user-b")
        assertFalse(second.isCompleted())
    }
}
