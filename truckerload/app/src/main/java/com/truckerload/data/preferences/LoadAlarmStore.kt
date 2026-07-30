package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class PendingLoadAlarm(
    val loadId: String,
    val tripId: String,
    val triggerAtMillis: Long,
    val pickupMillis: Long,
    val routeLabel: String,
)

/**
 * Persists pickup alarms so they can be restored after reboot.
 * Device-scoped (not per-user): alarms fire for the phone regardless of active account.
 */
class LoadAlarmStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<PendingLoadAlarm> {
        val raw = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        PendingLoadAlarm(
                            loadId = obj.getString("loadId"),
                            tripId = obj.optString("tripId"),
                            triggerAtMillis = obj.getLong("triggerAtMillis"),
                            pickupMillis = obj.getLong("pickupMillis"),
                            routeLabel = obj.optString("routeLabel"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun upsert(alarm: PendingLoadAlarm) {
        val updated = getAll().filterNot { it.loadId == alarm.loadId } + alarm
        saveAll(updated)
    }

    fun remove(loadId: String) {
        saveAll(getAll().filterNot { it.loadId == loadId })
    }

    private fun saveAll(alarms: List<PendingLoadAlarm>) {
        val arr = JSONArray()
        alarms.forEach { alarm ->
            arr.put(
                JSONObject()
                    .put("loadId", alarm.loadId)
                    .put("tripId", alarm.tripId)
                    .put("triggerAtMillis", alarm.triggerAtMillis)
                    .put("pickupMillis", alarm.pickupMillis)
                    .put("routeLabel", alarm.routeLabel),
            )
        }
        prefs.edit { putString(KEY_ALARMS, arr.toString()) }
    }

    companion object {
        private const val PREFS_NAME = "truckerload_load_alarms"
        private const val KEY_ALARMS = "pending_alarms"
    }
}
