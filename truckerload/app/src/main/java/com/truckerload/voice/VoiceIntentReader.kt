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
        val feature = extras.spoken("feature", "featureName")
        val recipient = extras.spoken("message.recipient.name", "recipientName", "peer")
        val text = extras.spoken("message.text", "messageText", "text")
        val callee = extras.spoken("call.callee.name", "calleeName")
        val thing = extras.spoken("thing.name", "name", "q")
        return when {
            !callee.isNullOrBlank() -> AppVoiceAction.CallFriend(callee)
            !recipient.isNullOrBlank() && !text.isNullOrBlank() ->
                AppVoiceAction.MessageFriend(recipient, text)
            !recipient.isNullOrBlank() || !thing.isNullOrBlank() ->
                AppVoiceAction.ChatWithFriend(recipient ?: thing!!)
            !feature.isNullOrBlank() -> AppVoiceActions.matchSpoken(feature)
            else -> null
        }
    }

    private fun Bundle.spoken(vararg keys: String): String? {
        keys.forEach { key ->
            val value = getString(key)?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return null
    }
}
