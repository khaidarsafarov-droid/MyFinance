package com.truckerload.data.preferences

import androidx.datastore.preferences.core.emptyPreferences
import com.truckerload.domain.week.WeekStartDay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
@Config(sdk = [28])
class SettingsScreenSnapshotTest {

    private lateinit var authStore: AuthStore
    private lateinit var profiles: UserProfileStore
    private lateinit var store: SettingsDataStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        authStore = AuthStore(context)
        profiles = UserProfileStore(context)
        store = SettingsDataStore(context)
        authStore.logout()
    }

    @After
    fun tearDown() = runBlocking {
        restoreDefaults()
        authStore.logout()
    }

    @Test
    fun emptyPreferencesUseTheSameDefaultsAsTheSeparateFlows() {
        val snapshot = store.readScreenSnapshot(emptyPreferences())
        assertEquals(SettingsScreenSnapshot(), snapshot)
        assertEquals(AppThemeMode.SYSTEM, snapshot.themeMode)
        assertFalse(snapshot.oledDark)
        assertFalse(snapshot.dynamicColor)
        assertFalse(snapshot.reduceMotion)
        assertFalse(snapshot.quietHoursEnabled)
        assertEquals(22, snapshot.quietHoursStart)
        assertEquals(7, snapshot.quietHoursEnd)
        assertTrue(snapshot.notifyMissingWeek)
        assertTrue(snapshot.notifyMaintenance)
        assertEquals(WeekStartDay.DEFAULT, snapshot.loadWeekStartDay)
        assertEquals(WeekStartDay.DEFAULT, snapshot.dieselWeekStartDay)
    }

    @Test
    fun snapshotMatchesSeparateFlowsAfterWrites() = runBlocking {
        store.saveThemeMode(AppThemeMode.DARK)
        store.saveOledDark(true)
        store.saveDynamicColor(true)
        store.saveReduceMotion(true)
        store.saveQuietHoursEnabled(true)
        store.saveQuietHoursStart(21)
        store.saveQuietHoursEnd(6)
        store.saveNotifyMissingWeek(false)
        store.saveNotifyMaintenance(false)
        store.saveLoadWeekStartDay(WeekStartDay.MONDAY)
        store.saveDieselWeekStartDay(WeekStartDay.SATURDAY)

        assertMatchesSeparateFlows(store.screenSnapshot.first())
    }

    @Test
    fun snapshotMatchesSeparateFlowsForTheSignedInAccount() = runBlocking {
        AuthLogin.completeLogin(
            authStore = authStore,
            userProfileStore = profiles,
            userId = "snapshot-user",
            profile = UserProfile(
                email = "snapshot@example.com",
                givenName = "Snap",
                familyName = "Shot",
                photoUrl = null,
            ),
        )
        store.saveThemeMode(AppThemeMode.LIGHT)
        store.saveOledDark(false)
        store.saveDynamicColor(true)
        store.saveReduceMotion(true)
        store.saveQuietHoursEnabled(true)
        store.saveQuietHoursStart(20)
        store.saveQuietHoursEnd(5)
        store.saveNotifyMissingWeek(false)
        store.saveNotifyMaintenance(true)
        store.saveLoadWeekStartDay(WeekStartDay.WEDNESDAY)
        store.saveDieselWeekStartDay(WeekStartDay.FRIDAY)

        assertMatchesSeparateFlows(store.screenSnapshot.first())
    }

    private suspend fun assertMatchesSeparateFlows(snapshot: SettingsScreenSnapshot) {
        assertEquals(store.themeMode.first(), snapshot.themeMode)
        assertEquals(store.oledDark.first(), snapshot.oledDark)
        assertEquals(store.dynamicColor.first(), snapshot.dynamicColor)
        assertEquals(store.reduceMotion.first(), snapshot.reduceMotion)
        assertEquals(store.quietHoursEnabled.first(), snapshot.quietHoursEnabled)
        assertEquals(store.quietHoursStart.first(), snapshot.quietHoursStart)
        assertEquals(store.quietHoursEnd.first(), snapshot.quietHoursEnd)
        assertEquals(store.notifyMissingWeek.first(), snapshot.notifyMissingWeek)
        assertEquals(store.notifyMaintenance.first(), snapshot.notifyMaintenance)
        assertEquals(store.loadWeekStartDay.first(), snapshot.loadWeekStartDay)
        assertEquals(store.dieselWeekStartDay.first(), snapshot.dieselWeekStartDay)
    }

    private suspend fun restoreDefaults() {
        store.saveThemeMode(AppThemeMode.SYSTEM)
        store.saveOledDark(false)
        store.saveDynamicColor(false)
        store.saveReduceMotion(false)
        store.saveQuietHoursEnabled(false)
        store.saveQuietHoursStart(22)
        store.saveQuietHoursEnd(7)
        store.saveNotifyMissingWeek(true)
        store.saveNotifyMaintenance(true)
        store.saveLoadWeekStartDay(WeekStartDay.DEFAULT)
        store.saveDieselWeekStartDay(WeekStartDay.DEFAULT)
    }
}
