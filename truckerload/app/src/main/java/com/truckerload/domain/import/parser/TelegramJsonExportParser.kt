package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.TelegramStyledTextNormalizer
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Parses result.json from Telegram Desktop chat export. */
class TelegramJsonExportParser(
    private val relayParser: LoadParser = RelayMessageParser(),
) : LoadParser {

    override fun parse(input: String): List<Load> {
        val root = JSONObject(input)
        val messages = root.optJSONArray("messages") ?: return emptyList()
        val byTrip = LinkedHashMap<String, Load>()

        for (i in 0 until messages.length()) {
            val msg = messages.optJSONObject(i) ?: continue
            if (msg.optString("type") != "message") continue

            val rawText = extractMessageText(msg)
            if (rawText.isBlank()) continue

            val text = TelegramStyledTextNormalizer.normalize(rawText)
            if (!MessageClassifier.isLoadLike(text)) continue

            // FIX: use Telegram message date so Relay MM/DD anchors to the export year
            val dateSeconds = extractMessageDateSeconds(msg)
            val parsedAtMs = dateSeconds?.times(1000L) ?: 0L

            relayParser.parse(text).forEach { load ->
                val keyed = load.tripId.uppercase(Locale.US)
                // FIX: last wins — Desktop exports are oldest-first; later rate/stop updates must win
                byTrip[keyed] = load.copy(
                    rawMessage = text.take(2000),
                    parsedAt = parsedAtMs,
                )
            }
        }

        return byTrip.values.toList()
    }

    private fun extractMessageDateSeconds(msg: JSONObject): Long? {
        if (!msg.has("date") || msg.isNull("date")) return null
        return when (val value = msg.opt("date")) {
            is Number -> value.toLong().takeIf { it > 0L }
            is String -> value.toLongOrNull()?.takeIf { it > 0L }
            else -> null
        }
    }

    private fun extractMessageText(msg: JSONObject): String {
        if (!msg.has("text") || msg.isNull("text")) return ""
        return when (val value = msg.opt("text")) {
            is String -> value
            is JSONArray -> flattenTextArray(value)
            else -> ""
        }
    }

    private fun flattenTextArray(arr: JSONArray): String = buildString {
        for (i in 0 until arr.length()) {
            when (val item = arr.opt(i)) {
                is String -> append(item)
                is JSONObject -> append(item.optString("text"))
            }
        }
    }

    companion object {
        fun isTelegramJsonExport(json: String): Boolean = try {
            val root = JSONObject(json.trim())
            root.has("messages") &&
                root.optJSONArray("messages") != null &&
                (root.has("name") || root.has("type") || root.has("id"))
        } catch (e: Exception) {
            false
        }
    }
}
