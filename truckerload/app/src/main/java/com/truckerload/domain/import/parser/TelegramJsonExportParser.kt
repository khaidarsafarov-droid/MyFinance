package com.truckerload.domain.import.parser

import com.truckerload.domain.import.ImportTripDedup
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.TelegramMessageDate
import com.truckerload.domain.parser.TelegramStyledTextNormalizer
import org.json.JSONArray
import org.json.JSONObject

/** Parses result.json from Telegram Desktop chat export. */
class TelegramJsonExportParser(
    private val relayParser: LoadParser = RelayMessageParser(),
) : LoadParser {

    override fun parse(input: String): List<Load> {
        val root = JSONObject(input)
        val messages = root.optJSONArray("messages") ?: return emptyList()
        val loads = mutableListOf<Load>()

        for (i in 0 until messages.length()) {
            val msg = messages.optJSONObject(i) ?: continue
            if (msg.optString("type") != "message") continue

            val rawText = extractMessageText(msg)
            if (rawText.isBlank()) continue

            val text = TelegramStyledTextNormalizer.normalize(rawText)
            if (!MessageClassifier.isLoadLike(text)) continue

            // FIX: Telegram Desktop uses date_unixtime + ISO date string, not unix in `date`
            val dateSeconds = extractMessageDateSeconds(msg)
            val parsedAtMs = dateSeconds?.times(1000L) ?: 0L
            val referenceMillis = parsedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()

            relayParser.parse(text, referenceMillis).forEach { load ->
                loads.add(
                    load.copy(
                        rawMessage = text.take(2000),
                        parsedAt = parsedAtMs.takeIf { it > 0L } ?: load.parsedAt,
                    )
                )
            }
        }

        // FIX: keep latest Trip ID revision — first-wins dropped rate/route updates
        return ImportTripDedup.keepLatestByTripId(loads)
    }

    private fun extractMessageDateSeconds(msg: JSONObject): Long? {
        TelegramMessageDate.parseUnixSeconds(msg.opt("date_unixtime"))?.let { return it }
        if (!msg.has("date") || msg.isNull("date")) return null
        return TelegramMessageDate.parseUnixSeconds(msg.opt("date"))
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
