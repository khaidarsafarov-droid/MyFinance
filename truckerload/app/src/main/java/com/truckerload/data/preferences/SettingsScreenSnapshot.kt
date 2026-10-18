package com.truckerload.data.preferences

import com.truckerload.domain.week.WeekStartDay

/**
 * Settings screen fields read from one DataStore emission.
 * Defaults match the values [com.truckerload.presentation.screens.settings.SettingsScreen]
 * used to pass to each flow before the first emission.
 */
data class SettingsScreenSnapshot(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val oledDark: Boolean = false,
    val dynamicColor: Boolean = false,
    val reduceMotion: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    val notifyMissingWeek: Boolean = true,
    val notifyMaintenance: Boolean = true,
    val loadWeekStartDay: WeekStartDay = WeekStartDay.DEFAULT,
    val dieselWeekStartDay: WeekStartDay = WeekStartDay.DEFAULT,
)
