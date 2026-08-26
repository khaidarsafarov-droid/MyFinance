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
 * Account-scoped privacy / parser prefs must not leak across users on a shared device.
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
    fun parserAndCrowdPrefs_areIsolatedPerUser() = runBlocking {
        loginAs("user-a", "a@example.com")
        settings.saveParserAutoUpdate(false)
        settings.saveQuietHoursEnabled(true)
        settings.saveNotifyMaintenance(false)
        assertFalse(settings.getParserAutoUpdateOnce())
        assertTrue(settings.getQuietHoursEnabledOnce())
        assertFalse(settings.getNotifyMaintenanceOnce())

        authStore.logout()
        loginAs("user-b", "b@example.com")
        assertTrue(
            "User B gets default parser auto-update when unset",
            settings.getParserAutoUpdateOnce(),
        )
        assertFalse(
            "User B must not inherit User A quiet hours",
            settings.getQuietHoursEnabledOnce(),
        )
        assertTrue(
            "User B must not inherit User A maintenance-notify off",
            settings.getNotifyMaintenanceOnce(),
        )

        authStore.logout()
        loginAs("user-a", "a@example.com")
        assertFalse(settings.getParserAutoUpdateOnce())
        assertTrue(settings.getQuietHoursEnabledOnce())
        assertFalse(settings.getNotifyMaintenanceOnce())
    }

    @Test
    fun weekStartDays_areIsolatedPerUser() = runBlocking {
        loginAs("user-a", "a@example.com")
        settings.saveLoadWeekStartDay(com.truckerload.domain.week.WeekStartDay.MONDAY)
        settings.saveDieselWeekStartDay(com.truckerload.domain.week.WeekStartDay.SATURDAY)
        assertEquals(
            com.truckerload.domain.week.WeekStartDay.MONDAY,
            settings.getLoadWeekStartDayOnce(),
        )
        assertEquals(
            com.truckerload.domain.week.WeekStartDay.SATURDAY,
            settings.getDieselWeekStartDayOnce(),
        )

        authStore.logout()
        loginAs("user-b", "b@example.com")
        assertEquals(
            com.truckerload.domain.week.WeekStartDay.SUNDAY,
            settings.getLoadWeekStartDayOnce(),
        )
        assertEquals(
            com.truckerload.domain.week.WeekStartDay.SUNDAY,
            settings.getDieselWeekStartDayOnce(),
        )
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
