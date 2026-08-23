package com.truckerload.widget

import androidx.core.content.edit
import android.content.Context

object WidgetDeepLink {
    const val ROUTE_HOME = "home"
    /**
     * Opens Analytics (RPM / period stats). Value is **"analytics"** — not `"stats"` —
     * because [com.truckerload.presentation.navigation.Routes.STATS] hosts WeeklyGoalScreen.
     */
    const val ROUTE_STATS = "analytics"
    const val ROUTE_ADD_LOAD = "add_load"
    const val ROUTE_JOURNAL_THIS_WEEK = "journal_this_week"
    /** Opens WeeklyGoalScreen via [com.truckerload.presentation.navigation.Routes.STATS]. */
    const val ROUTE_WEEKLY_GOAL = "weekly_goal"
    const val ROUTE_CAMERA = "camera"
    const val ROUTE_SCANNER = "scanner"
    const val ROUTE_ADD_DIESEL = "add_diesel"
    /** Pick a recent load, then open camera attached to it. */
    const val ROUTE_ATTACH_CAMERA = "attach_pick/camera"
    /** Pick a recent load, then open scanner attached to it. */
    const val ROUTE_ATTACH_SCANNER = "attach_pick/scanner"
    private const val PREFS = "truckerload_widget"
    private const val KEY_OPEN_THIS_WEEK = "open_this_week"

    /**
     * Maps a widget / MainActivity EXTRA_ROUTE value to a NavGraph route string.
     * Pure helper for unit tests and to keep NavGraph branches consistent.
     *
     * Widget Camera/Scan open the load picker first so media is attached to a trip.
     */
    fun resolveNavRoute(deepLinkRoute: String): String? = when (deepLinkRoute) {
        ROUTE_HOME, "home" -> "home"
        ROUTE_ADD_LOAD, "add_load" -> "add_load"
        // Current + legacy widget RPM target (pre-rename value was "stats")
        ROUTE_STATS, "analytics", "stats" -> "analytics"
        ROUTE_JOURNAL_THIS_WEEK -> "home"
        ROUTE_WEEKLY_GOAL -> "stats" // WeeklyGoalScreen lives at Routes.STATS
        ROUTE_CAMERA, "camera", ROUTE_ATTACH_CAMERA -> ROUTE_ATTACH_CAMERA
        ROUTE_SCANNER, "scanner", ROUTE_ATTACH_SCANNER -> ROUTE_ATTACH_SCANNER
        ROUTE_ADD_DIESEL, "diesel" -> ROUTE_ADD_DIESEL
        else -> null
    }

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
