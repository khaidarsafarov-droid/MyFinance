package com.truckerload.data.preferences

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * QUALITY_100 #43 — theme mode survives DataStore round-trip (process death).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsDataStoreThemeTest {

    private lateinit var store: SettingsDataStore

    @Before
    fun setUp() {
        store = SettingsDataStore(RuntimeEnvironment.getApplication())
    }

    @Test
    fun saveThemeMode_roundTripsThroughGetThemeModeOnce() = runBlocking {
        store.saveThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, store.getThemeModeOnce())

        store.saveThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, store.getThemeModeOnce())

        store.saveThemeMode(AppThemeMode.SYSTEM)
        assertEquals(AppThemeMode.SYSTEM, store.getThemeModeOnce())
    }

    @Test
    fun saveReduceMotion_roundTripsThroughGetReduceMotionOnce() = runBlocking {
        store.saveReduceMotion(true)
        assertEquals(true, store.getReduceMotionOnce())

        store.saveReduceMotion(false)
        assertEquals(false, store.getReduceMotionOnce())
    }
}
