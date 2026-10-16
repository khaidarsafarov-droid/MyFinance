package com.truckerload.sync

import android.content.Context
import androidx.core.content.edit
import com.truckerload.domain.model.Load

/** The last load this phone saved from the Telegram chat, so /undo and /rate can find it. */
internal object TelegramLastLoad {
    fun remember(context: Context, load: Load) {
        if (load.id.isBlank()) return
        prefs(context).edit {
            putString(ID, load.id)
            putString(TRIP, load.tripId)
        }
    }

    fun id(context: Context): String? = prefs(context).getString(ID, null)?.takeIf { it.isNotBlank() }

    fun tripId(context: Context): String? = prefs(context).getString(TRIP, null)?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        prefs(context).edit {
            remove(ID)
            remove(TRIP)
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "telegram_last_load"
    private const val ID = "id"
    private const val TRIP = "trip"
}
