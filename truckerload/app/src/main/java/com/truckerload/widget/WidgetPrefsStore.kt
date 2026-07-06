package com.truckerload.widget

import androidx.core.content.edit
import android.content.Context

enum class WidgetSizeMode {
    AUTO, SMALL, MEDIUM, LARGE
}

enum class WidgetThemeMode {
    SYSTEM, LIGHT, DARK
}

data class WidgetPrefs(
    val sizeMode: WidgetSizeMode = WidgetSizeMode.AUTO,
    val showGross: Boolean = true,
    val showPace: Boolean = true,
    val showGoal: Boolean = true,
    val themeMode: WidgetThemeMode = WidgetThemeMode.SYSTEM
)

object WidgetPrefsStore {
    private const val PREFS = "truckerload_widget_config"

    fun load(context: Context, appWidgetId: Int): WidgetPrefs {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "w$appWidgetId."
        return WidgetPrefs(
            sizeMode = WidgetSizeMode.entries.getOrElse(
                prefs.getInt("${prefix}size", WidgetSizeMode.AUTO.ordinal)
            ) { WidgetSizeMode.AUTO },
            showGross = prefs.getBoolean("${prefix}gross", true),
            showPace = prefs.getBoolean("${prefix}pace", true),
            showGoal = prefs.getBoolean("${prefix}goal", true),
            themeMode = WidgetThemeMode.entries.getOrElse(
                prefs.getInt("${prefix}theme", WidgetThemeMode.SYSTEM.ordinal)
            ) { WidgetThemeMode.SYSTEM }
        )
    }

    fun save(context: Context, appWidgetId: Int, prefs: WidgetPrefs) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putInt("w$appWidgetId.size", prefs.sizeMode.ordinal)
                putBoolean("w$appWidgetId.gross", prefs.showGross)
                putBoolean("w$appWidgetId.pace", prefs.showPace)
                putBoolean("w$appWidgetId.goal", prefs.showGoal)
                putInt("w$appWidgetId.theme", prefs.themeMode.ordinal)
            }
    }

    fun delete(context: Context, appWidgetId: Int) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            remove("w$appWidgetId.size")
            remove("w$appWidgetId.gross")
            remove("w$appWidgetId.pace")
            remove("w$appWidgetId.goal")
            remove("w$appWidgetId.theme")
        }
    }
}
