package com.truckerload.widget

import android.content.Context
import androidx.core.content.edit
import androidx.glance.GlanceId

/** Per-widget selected weekday (0 = Sunday … 6 = Saturday). Null = today. */
object WidgetDaySelectionStore {
    private const val PREFS = "truckerload_widget_day_sel"

    fun save(context: Context, glanceId: GlanceId, offset: Int) {
        prefs(context).edit { putInt(key(glanceId), offset.coerceIn(0, 6)) }
    }

    fun load(context: Context, glanceId: GlanceId): Int? {
        val stored = prefs(context)
        val name = key(glanceId)
        if (!stored.contains(name)) return null
        return stored.getInt(name, 0).coerceIn(0, 6)
    }

    private fun key(glanceId: GlanceId): String = glanceId.toString()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
