package com.truckerload.widget

import androidx.core.content.edit
import android.content.Context

object WidgetDeepLink {
    const val ROUTE_HOME = "home"
    const val ROUTE_STATS = "stats"
    const val ROUTE_ADD_LOAD = "add_load"
    const val ROUTE_JOURNAL_THIS_WEEK = "journal_this_week"
    const val ROUTE_WEEKLY_GOAL = "weekly_goal"
    const val ROUTE_CAMERA = "camera"
    const val ROUTE_SCANNER = "scanner"
    private const val PREFS = "truckerload_widget"
    private const val KEY_OPEN_THIS_WEEK = "open_this_week"

    fun markOpenJournalThisWeek(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_OPEN_THIS_WEEK, true)
            }
    }

    fun consumeOpenJournalThisWeek(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val open = prefs.getBoolean(KEY_OPEN_THIS_WEEK, false)
        if (open) {
            prefs.edit {remove(KEY_OPEN_THIS_WEEK)}
        }
        return open
    }
}
