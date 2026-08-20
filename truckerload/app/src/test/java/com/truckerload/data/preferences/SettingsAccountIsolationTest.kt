package com.truckerload.data.preferences

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Stage3: account-scoped privacy / parser prefs must not leak across users
 * on a shared device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsAccountIsolationTest {

    private lateinit var context: Context
    private lateinit var authStore: AuthStore
    private lateinit var profiles: UserProfileStore
    private lateinit var settings: SettingsDataStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        authStore = AuthStore(context)
        profiles = UserProfileStore(context)
        settings = SettingsDataStore(context)
        authStore.logout()
    }

    @Test
    fun sharePath_andParserPrefs_areIsolatedPerUser() = runBlocking {
        loginAs("user-a", "a@example.com")
        settings.saveSharePathWithFriends(true)
        settings.saveParserAutoUpdate(false)
        settings.saveQuietHoursEnabled(true)
        settings.saveCrowdStatsOptIn(true)
        assertTrue(settings.getSharePathWithFriendsOnce())
        assertFalse(settings.getParserAutoUpdateOnce())
        assertTrue(settings.getQuietHoursEnabledOnce())
        assertTrue(settings.getCrowdStatsOptInOnce())
        assertTrue(settings.isCrowdStatsPromptSeenOnce())

        authStore.logout()
        loginAs("user-b", "b@example.com")
        assertFalse(
            "User B must not inherit User A share-path opt-in",
            settings.getSharePathWithFriendsOnce(),
        )
        assertTrue(
            "User B gets default parser auto-update when unset",
            settings.getParserAutoUpdateOnce(),
        )
        assertFalse(
            "User B must not inherit User A quiet hours",
            settings.getQuietHoursEnabledOnce(),
        )
        assertFalse(
            "User B must not inherit User A crowd RPM opt-in",
            settings.getCrowdStatsOptInOnce(),
        )

        settings.saveSharePathWithFriends(true)
        assertTrue(settings.getSharePathWithFriendsOnce())

        authStore.logout()
        loginAs("user-a", "a@example.com")
        assertTrue(settings.getSharePathWithFriendsOnce())
        assertFalse(settings.getParserAutoUpdateOnce())
    }

    @Test
    fun friendsLocationInterval_andLiveMode_defaultAndIsolated() = runBlocking {
        loginAs("user-a", "a@example.com")
        assertEquals(30, settings.getFriendsLocationIntervalMinutesOnce())
        assertFalse(settings.getFriendsLiveModeOnce())
        settings.saveFriendsLocationIntervalMinutes(15)
        settings.saveFriendsLiveMode(true)
        assertEquals(15, settings.getFriendsLocationIntervalMinutesOnce())
        assertTrue(settings.getFriendsLiveModeOnce())

        authStore.logout()
        loginAs("user-b", "b@example.com")
        assertEquals(30, settings.getFriendsLocationIntervalMinutesOnce())
        assertFalse(settings.getFriendsLiveModeOnce())
    }

    private fun loginAs(userId: String, email: String) {
        AuthLogin.completeLogin(
            authStore = authStore,
            userProfileStore = profiles,
            userId = userId,
            profile = UserProfile(
                email = email,
                givenName = "Test",
                familyName = "User",
                photoUrl = null,
            ),
        )
    }
}
