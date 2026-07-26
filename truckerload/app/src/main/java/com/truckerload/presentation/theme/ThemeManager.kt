package com.truckerload.presentation.theme

import androidx.appcompat.app.AppCompatDelegate
import com.truckerload.data.preferences.AppThemeMode

object ThemeManager {

    fun nightModeFor(mode: AppThemeMode): Int =
        when (mode) {
            AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }

    /**
     * Applies night mode only when it actually changes.
     * Calling [AppCompatDelegate.setDefaultNightMode] with a new value recreates
     * activities; re-applying the Compose [initialValue] (SYSTEM) after a Light/Dark
     * save was recreating in a loop and left MainActivity stuck on the spinner.
     */
    fun apply(mode: AppThemeMode) {
        val target = nightModeFor(mode)
        if (AppCompatDelegate.getDefaultNightMode() == target) return
        AppCompatDelegate.setDefaultNightMode(target)
    }
}
