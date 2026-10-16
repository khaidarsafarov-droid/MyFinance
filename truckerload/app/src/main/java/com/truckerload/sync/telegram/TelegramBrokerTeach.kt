package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.R
import com.truckerload.domain.parser.BrokerLabelLearner
import org.json.JSONObject

/**
 * /teach: the driver pastes a rate confirmation, then the load number and the rate
 * as printed. The labels in front of those values are remembered for next time.
 */
class TelegramBrokerTeach(
    private val context: Context,
    private val apiClient: TelegramApiClient,
) {
    suspend fun handleCommand(text: String, chatId: String, prefs: SharedPreferences): Boolean {
        if (!isTeach(text)) return false
        save(prefs, chatId, phase = PHASE_DOCUMENT, body = "", tripLabel = "")
        apiClient.sendWithMenu(chatId, context.getString(R.string.sync_teach_start))
        return true
    }

    suspend fun continueSession(text: String, chatId: String, prefs: SharedPreferences): Boolean {
        val session = load(prefs, chatId) ?: return false
        val reply = text.trim()
        if (reply.startsWith("/")) {
            clear(prefs, chatId)
            return false
        }
        when (session.phase) {
            PHASE_DOCUMENT -> {
                save(prefs, chatId, PHASE_TRIP, reply, "")
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_teach_ask_trip))
            }
            PHASE_TRIP -> {
                val label = BrokerLabelLearner.labelBefore(session.body, reply)
                if (label == null) {
                    apiClient.sendWithMenu(chatId, context.getString(R.string.sync_teach_miss))
                    return true
                }
                save(prefs, chatId, PHASE_RATE, session.body, label)
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_teach_ask_rate))
            }
            PHASE_RATE -> {
                val rateLabel = BrokerLabelLearner.labelBefore(session.body, reply)
                if (rateLabel == null) {
                    apiClient.sendWithMenu(chatId, context.getString(R.string.sync_teach_miss))
                    return true
                }
                UserBrokerTemplateStore.add(
                    context,
                    UserBrokerTemplateStore.template(session.tripLabel, rateLabel),
                )
                clear(prefs, chatId)
                apiClient.sendWithMenu(
                    chatId,
                    context.getString(R.string.sync_teach_saved, session.tripLabel, rateLabel),
                )
            }
        }
        return true
    }

    fun clear(prefs: SharedPreferences, chatId: String): Boolean {
        val had = prefs.contains(key(chatId))
        prefs.edit { remove(key(chatId)) }
        return had
    }

    private fun save(
        prefs: SharedPreferences,
        chatId: String,
        phase: String,
        body: String,
        tripLabel: String,
    ) {
        val json = JSONObject()
            .put("phase", phase)
            .put("body", body.take(12_000))
            .put("trip", tripLabel)
        prefs.edit(commit = true) { putString(key(chatId), json.toString()) }
    }

    private fun load(prefs: SharedPreferences, chatId: String): Session? {
        val raw = prefs.getString(key(chatId), null) ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        return Session(
            phase = json.optString("phase"),
            body = json.optString("body"),
            tripLabel = json.optString("trip"),
        )
    }

    private fun isTeach(text: String): Boolean {
        val head = text.trim().substringBefore(' ')
        return head.equals("/teach", ignoreCase = true) ||
            head.startsWith("/teach@", ignoreCase = true)
    }

    private fun key(chatId: String) = "tg_teach_$chatId"

    private data class Session(val phase: String, val body: String, val tripLabel: String)

    private companion object {
        const val PHASE_DOCUMENT = "document"
        const val PHASE_TRIP = "trip"
        const val PHASE_RATE = "rate"
    }
}
