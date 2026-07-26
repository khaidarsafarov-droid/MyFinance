package com.truckerload.presentation.theme

import androidx.appcompat.app.AppCompatDelegate
import com.truckerload.data.preferences.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThemeManagerTest {

    @Before
    fun setUp() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    @Test
    fun nightModeFor_mapsModes() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ThemeManager.nightModeFor(AppThemeMode.SYSTEM),
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            ThemeManager.nightModeFor(AppThemeMode.LIGHT),
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            ThemeManager.nightModeFor(AppThemeMode.DARK),
        )
    }

    @Test
    fun apply_isIdempotent_doesNotResetAfterLight() {
        ThemeManager.apply(AppThemeMode.LIGHT)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())

        // Re-applying the same mode must be a no-op (would otherwise recreate activities).
        ThemeManager.apply(AppThemeMode.LIGHT)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun apply_switchesBetweenLightAndSystem() {
        ThemeManager.apply(AppThemeMode.LIGHT)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())

        ThemeManager.apply(AppThemeMode.SYSTEM)
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.getDefaultNightMode(),
        )
    }
}
