package com.truckerload.voice

import android.content.Intent
import android.os.Bundle

object VoiceIntentReader {
    fun parse(intent: Intent?): AppVoiceAction? {
        if (intent == null) return null
        val data = intent.dataString
        if (!data.isNullOrBlank() && data.startsWith("${AppVoiceActions.SCHEME}://")) {
            return AppVoiceActions.parseUri(data)
        }
        val extras = intent.extras ?: return null
        return fromExtras(extras)
    }

    private fun fromExtras(extras: Bundle): AppVoiceAction? {
        val keys = extras.toLookup()
        val pathHint = keys.firstOf("path", "capability")
        AppVoiceJournal.actionFromPath(pathHint.orEmpty(), keys)?.let { return it }
        if (keys.firstOf("amount", "gallons", "date") != null &&
            AppVoiceJournal.isWeeklyGrossQuery(keys.firstOf("q", "query", "feature", "featurename").orEmpty()).not()
        ) {
            val feature = keys.firstOf("feature", "featurename", "q").orEmpty()
            val asDiesel = feature.contains("paycheck", ignoreCase = true).not() &&
                (feature.contains("diesel", ignoreCase = true) ||
                    feature.contains("дизель") ||
                    keys.containsKey("gallons"))
            val asPaycheck = feature.contains("paycheck", ignoreCase = true) ||
                feature.contains("зарплат") ||
                feature.contains("pay")
            when {
                asPaycheck -> return AppVoiceJournal.addPaycheck(keys)
                asDiesel -> return AppVoiceJournal.addDiesel(keys)
            }
        }
        val recipient = keys.firstOf("message.recipient.name", "recipientname", "peer")
        val text = keys.firstOf("message.text", "messagetext", "text")
        val callee = keys.firstOf("call.callee.name", "calleename")
        val thing = keys.firstOf("thing.name", "q", "name")
        val feature = keys.firstOf("feature", "featurename")
        return when {
            !callee.isNullOrBlank() -> AppVoiceAction.CallFriend(callee)
            !recipient.isNullOrBlank() && !text.isNullOrBlank() ->
                AppVoiceAction.MessageFriend(recipient, text)
            !recipient.isNullOrBlank() -> AppVoiceAction.ChatWithFriend(recipient)
            !thing.isNullOrBlank() -> AppVoiceJournal.fromSpokenQuery(thing)
                ?: AppVoiceActions.matchSpoken(thing)
            !feature.isNullOrBlank() -> AppVoiceJournal.fromSpokenQuery(feature)
                ?: AppVoiceActions.matchSpoken(feature)
            else -> null
        }
    }

    private fun Bundle.toLookup(): Map<String, String> {
        val out = linkedMapOf<String, String>()
        keySet().orEmpty().forEach { key ->
            val value = getString(key)?.trim().orEmpty()
            if (key.isNotBlank() && value.isNotBlank()) {
                out[key.lowercase()] = value
            }
        }
        return out
    }

    private fun Map<String, String>.firstOf(vararg keys: String): String? {
        keys.forEach { key ->
            val value = this[key.lowercase()]?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return null
    }
}
