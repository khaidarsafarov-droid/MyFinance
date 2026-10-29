package com.truckerload.sync

import android.content.Context
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner.FireState
import com.truckerload.domain.maintenance.MaintenanceReminderPlanner.Occurrence
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-protected copy of armed reminders so a locked boot can re-arm alarms
 * before credential-encrypted Room is available.
 */
class MaintenanceAlarmStore(context: Context) {

    private val prefs = context.applicationContext
        .createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(userId: String): UserAlarms {
        val root = root()
        val user = root.optJSONObject(userId) ?: return UserAlarms()
        return UserAlarms(
            states = user.optJSONObject("states")?.toStates().orEmpty(),
            alarms = user.optJSONArray("alarms")?.toAlarms().orEmpty(),
            pending = user.optJSONArray("pending")?.toPending().orEmpty(),
        )
    }

    fun write(userId: String, alarms: UserAlarms) {
        val root = root()
        root.put(userId, alarms.toJson())
        prefs.edit().putString(KEY_ROOT, root.toString()).apply()
    }

    fun allAlarms(): List<Pair<String, Occurrence>> {
        val root = root()
        val out = ArrayList<Pair<String, Occurrence>>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val userId = keys.next()
            val user = root.optJSONObject(userId) ?: continue
            user.optJSONArray("alarms")?.toAlarms()?.forEach { out += userId to it }
        }
        return out
    }

    fun isFlagSet(key: String): Boolean = prefs.getBoolean(key, false)

    fun setFlag(key: String) {
        prefs.edit().putBoolean(key, true).apply()
    }

    data class UserAlarms(
        val states: Map<Long, FireState> = emptyMap(),
        val alarms: List<Occurrence> = emptyList(),
        val pending: List<PendingAction> = emptyList(),
    ) {
        fun alarm(taskId: Long): Occurrence? = alarms.find { it.alarmId == taskId }
    }

    data class PendingAction(
        val taskId: Long,
        val kind: Kind,
        val atMillis: Long,
    ) {
        enum class Kind { DONE, SNOOZE }
    }

    private fun root(): JSONObject =
        runCatching { JSONObject(prefs.getString(KEY_ROOT, null) ?: "{}") }
            .getOrDefault(JSONObject())

    private fun UserAlarms.toJson(): JSONObject = JSONObject()
        .put("states", JSONObject().apply {
            states.forEach { (id, state) -> put(id.toString(), state.toJson()) }
        })
        .put("alarms", JSONArray().apply { alarms.forEach { put(it.toJson()) } })
        .put("pending", JSONArray().apply { pending.forEach { put(it.toJson()) } })

    private fun FireState.toJson(): JSONObject = JSONObject()
        .put("fingerprint", fingerprint)
        .putOpt("lastFiredCycleKey", lastFiredCycleKey)
        .putOpt("consumedDue", consumedDue)
        .putOpt("snoozeUntilMillis", snoozeUntilMillis)
        .putOpt("nextNagAtMillis", nextNagAtMillis)

    private fun Occurrence.toJson(): JSONObject = JSONObject()
        .put("alarmId", alarmId)
        .put("taskId", taskId)
        .put("triggerAtMillis", triggerAtMillis)
        .put("cycleKey", cycleKey)
        .put("title", title)
        .put("alerts", alerts)
        .put("replay", replay)

    private fun PendingAction.toJson(): JSONObject = JSONObject()
        .put("taskId", taskId)
        .put("kind", kind.name)
        .put("atMillis", atMillis)

    private fun JSONObject.toStates(): Map<Long, FireState> {
        val out = LinkedHashMap<Long, FireState>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val id = key.toLongOrNull() ?: continue
            val obj = optJSONObject(key) ?: continue
            out[id] = FireState(
                fingerprint = obj.optString("fingerprint"),
                lastFiredCycleKey = obj.optString("lastFiredCycleKey").takeIf { it.isNotEmpty() },
                consumedDue = obj.optString("consumedDue").takeIf { it.isNotEmpty() },
                snoozeUntilMillis = obj.optPositive("snoozeUntilMillis"),
                nextNagAtMillis = obj.optPositive("nextNagAtMillis"),
            )
        }
        return out
    }

    private fun JSONArray.toAlarms(): List<Occurrence> = buildList {
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            add(
                Occurrence(
                    alarmId = obj.optLong("alarmId"),
                    taskId = obj.optLong("taskId"),
                    triggerAtMillis = obj.optLong("triggerAtMillis"),
                    cycleKey = obj.optString("cycleKey"),
                    title = obj.optString("title"),
                    alerts = obj.optBoolean("alerts"),
                    replay = obj.optBoolean("replay"),
                ),
            )
        }
    }

    private fun JSONArray.toPending(): List<PendingAction> = buildList {
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            val kind = runCatching { PendingAction.Kind.valueOf(obj.optString("kind")) }.getOrNull()
                ?: continue
            add(
                PendingAction(
                    taskId = obj.optLong("taskId"),
                    kind = kind,
                    atMillis = obj.optLong("atMillis"),
                ),
            )
        }
    }

    private fun JSONObject.optPositive(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val value = optLong(key, Long.MIN_VALUE)
        return value.takeIf { it > 0L }
    }

    companion object {
        const val PREFS = "maintenance_reminder_alarms"
        const val FLAG_EXACT_PROMPT = "exact_prompt_shown"
        const val FLAG_BATTERY_PROMPT = "battery_prompt_shown"
        const val FLAG_NOTIF_ASKED = "notification_permission_asked"
        private const val KEY_ROOT = "root"
    }
}
