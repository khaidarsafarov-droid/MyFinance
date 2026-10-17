package com.truckerload.sync.telegram

import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.domain.parser.LoadDraftFields
import org.json.JSONObject

/**
 * A rate confirmation that had a rate or a city but could not be saved yet.
 * The next short reply fills the missing piece.
 */
class TelegramIncompleteLoadStore(
    private val prefs: SharedPreferences,
) {
    fun save(chatId: String, draft: LoadDraftFields, rawText: String, messageDateSeconds: Long?) {
        val json = JSONObject()
            .put("trip", draft.tripId)
            .put("date", draft.date)
            .put("rate", draft.rate)
            .put("miles", draft.miles)
            .put("pointA", draft.pointA)
            .put("pointB", draft.pointB)
            .put("text", rawText.take(12_000))
        if (messageDateSeconds != null) json.put("msgDate", messageDateSeconds)
        prefs.edit(commit = true) {
            putString(key(chatId), json.toString())
            putLong(timeKey(chatId), System.currentTimeMillis())
        }
    }

    fun load(chatId: String): Saved? {
        val savedAt = prefs.getLong(timeKey(chatId), 0L)
        if (savedAt != 0L && System.currentTimeMillis() - savedAt > TTL_MS) {
            clear(chatId)
            return null
        }
        val raw = prefs.getString(key(chatId), null) ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val draft = LoadDraftFields(
            tripId = json.optString("trip"),
            date = json.optString("date"),
            rate = json.optString("rate"),
            miles = json.optString("miles"),
            pointA = json.optString("pointA"),
            pointB = json.optString("pointB"),
        )
        val msgDate = if (json.has("msgDate") && !json.isNull("msgDate")) json.optLong("msgDate") else null
        return Saved(draft = draft, rawText = json.optString("text"), messageDateSeconds = msgDate)
    }

    fun clear(chatId: String): Boolean {
        val had = prefs.contains(key(chatId))
        prefs.edit {
            remove(key(chatId))
            remove(timeKey(chatId))
        }
        return had
    }

    data class Saved(
        val draft: LoadDraftFields,
        val rawText: String,
        val messageDateSeconds: Long?,
    )

    private fun key(chatId: String) = KEY_PREFIX + chatId
    private fun timeKey(chatId: String) = TIME_PREFIX + chatId

    private companion object {
        const val TTL_MS = 30L * 60L * 1000L
        const val KEY_PREFIX = "tg_incomplete_"
        const val TIME_PREFIX = "tg_incomplete_at_"
    }
}
