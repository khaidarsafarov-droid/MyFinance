package com.truckerload.data.preferences

import org.junit.After
import org.junit.Assert.assertEquals
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
class LocalDeviceOnboardingTest {

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
    fun namesAreValid_requiresBothFirstAndLast() {
        assertFalse(LocalDeviceOnboarding.namesAreValid("", "Ivanov"))
        assertFalse(LocalDeviceOnboarding.namesAreValid("Ivan", "  "))
        assertTrue(LocalDeviceOnboarding.namesAreValid(" Ivan ", "Ivanov"))
    }

    @Test
    fun complete_opensLocalSessionAndMarksSetupDone() {
        val context = RuntimeEnvironment.getApplication()
        val auth = AuthStore(context)
        val profile = UserProfileStore(context)

        LocalDeviceOnboarding.complete(auth, profile, "Иван", "Иванов")

        assertTrue(auth.isLoggedIn.value)
        assertEquals(AccountIds.LOCAL_DEV, auth.currentUserIdOrNull())
        assertEquals(AuthProvider.LOCAL, auth.authProvider())
        assertEquals("Иван", profile.profile.value?.givenName)
        assertEquals("Иванов", profile.profile.value?.familyName)
        assertTrue(profile.setupComplete.value)
    }

    @Test
    fun complete_survivesProcessRestart() {
        val context = RuntimeEnvironment.getApplication()
        LocalDeviceOnboarding.complete(
            AuthStore(context),
            UserProfileStore(context),
            "Anna",
            "Petrova",
        )
        AuthStore.resetForTests()
        val restored = AuthStore(context)
        assertTrue(restored.isLoggedIn.value)
        assertEquals(AccountIds.LOCAL_DEV, restored.currentUserIdOrNull())
        assertEquals(AuthProvider.LOCAL, restored.authProvider())
    }

    @Test(expected = IllegalArgumentException::class)
    fun complete_rejectsBlankNames() {
        val context = RuntimeEnvironment.getApplication()
        LocalDeviceOnboarding.complete(AuthStore(context), UserProfileStore(context), " ", "Иванов")
    }
}
