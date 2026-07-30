package com.truckerload.sync

import android.content.Context

/** Persists pending load pickup alarms so they can be restored after reboot. */
class LoadAlarmStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Entry(
        val loadId: String,
        val tripId: String,
        val triggerAtMillis: Long,
        val pickupMillis: Long,
    )

    fun put(entry: Entry) {
        prefs.edit().putString(entry.loadId, encode(entry)).apply()
    }

    fun remove(loadId: String) {
        prefs.edit().remove(loadId).apply()
    }

    fun get(loadId: String): Entry? =
        prefs.getString(loadId, null)?.let { decode(loadId, it) }

    fun all(): List<Entry> =
        prefs.all.mapNotNull { (loadId, value) ->
            (value as? String)?.let { decode(loadId, it) }
        }

    private fun encode(entry: Entry): String =
        "${entry.triggerAtMillis}|${entry.pickupMillis}|${entry.tripId}"

    private fun decode(loadId: String, raw: String): Entry? {
        val parts = raw.split("|", limit = 3)
        if (parts.size < 3) return null
        val trigger = parts[0].toLongOrNull() ?: return null
        val pickup = parts[1].toLongOrNull() ?: return null
        return Entry(
            loadId = loadId,
            tripId = parts[2],
            triggerAtMillis = trigger,
            pickupMillis = pickup,
        )
    }

    companion object {
        private const val PREFS = "truckerload_load_alarms"
    }
}
