package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.presentation.screens.stats.StatsPeriod

private const val LEGACY_PREFS_NAME = "truckerload_settings"
private const val KEY_STATS_PERIOD = "stats_period"
private const val KEY_STATS_WEEK = "stats_week"
private const val KEY_STATS_WEEK_YEAR = "stats_week_year"
private const val KEY_STATS_CALENDAR_MONTH = "stats_calendar_month"
private const val KEY_STATS_CALENDAR_YEAR = "stats_calendar_year"

data class StatsSelectionSnapshot(
    val period: StatsPeriod,
    val weekNumber: Int,
    val weekYear: Int,
    val calendarMonth: Int,
    val calendarYear: Int
)

class StatsSelectionStore(
    context: Context,
    userId: String = AuthStore(context).currentUserIdOrNull() ?: AccountIds.LOCAL_DEV,
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "truckerload_stats_selection_${AccountIds.sanitizeFilePart(userId)}",
            Context.MODE_PRIVATE,
        ).also { scoped ->
            migrateFromLegacyIfEmpty(context.applicationContext, scoped)
        }

    fun read(defaultWeek: Int, defaultYear: Int, defaultMonth: Int): StatsSelectionSnapshot {
        val period = runCatching {
            StatsPeriod.valueOf(prefs.getString(KEY_STATS_PERIOD, StatsPeriod.WEEK.name).orEmpty())
        }.getOrElse { StatsPeriod.WEEK }

        val week = prefs.getInt(KEY_STATS_WEEK, defaultWeek).coerceIn(1, 53)
        val weekYear = prefs.getInt(KEY_STATS_WEEK_YEAR, defaultYear)
        val calendarMonth = prefs.getInt(KEY_STATS_CALENDAR_MONTH, defaultMonth).coerceIn(1, 12)
        val calendarYear = prefs.getInt(KEY_STATS_CALENDAR_YEAR, defaultYear)

        return StatsSelectionSnapshot(
            period = period,
            weekNumber = week,
            weekYear = weekYear,
            calendarMonth = calendarMonth,
            calendarYear = calendarYear
        )
    }

    fun save(snapshot: StatsSelectionSnapshot) {
        prefs.edit {
            putString(KEY_STATS_PERIOD, snapshot.period.name)
            putInt(KEY_STATS_WEEK, snapshot.weekNumber.coerceIn(1, 53))
            putInt(KEY_STATS_WEEK_YEAR, snapshot.weekYear)
            putInt(KEY_STATS_CALENDAR_MONTH, snapshot.calendarMonth.coerceIn(1, 12))
            putInt(KEY_STATS_CALENDAR_YEAR, snapshot.calendarYear)
        }
    }

    companion object {
        private fun migrateFromLegacyIfEmpty(context: Context, scoped: SharedPreferences) {
            if (scoped.contains(KEY_STATS_PERIOD)) return
            val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            if (!legacy.contains(KEY_STATS_PERIOD)) return
            scoped.edit {
                putString(KEY_STATS_PERIOD, legacy.getString(KEY_STATS_PERIOD, StatsPeriod.WEEK.name))
                putInt(KEY_STATS_WEEK, legacy.getInt(KEY_STATS_WEEK, 1))
                putInt(KEY_STATS_WEEK_YEAR, legacy.getInt(KEY_STATS_WEEK_YEAR, 2024))
                putInt(KEY_STATS_CALENDAR_MONTH, legacy.getInt(KEY_STATS_CALENDAR_MONTH, 1))
                putInt(KEY_STATS_CALENDAR_YEAR, legacy.getInt(KEY_STATS_CALENDAR_YEAR, 2024))
            }
        }
    }
}
