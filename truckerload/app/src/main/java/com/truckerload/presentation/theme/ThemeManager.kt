package com.truckerload.presentation.theme

import androidx.appcompat.app.AppCompatDelegate
import com.truckerload.data.preferences.AppThemeMode

object ThemeManager {

    fun apply(mode: AppThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }
}
