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
            ?: extras.spoken("thing.name", "name", "q")
        return if (!feature.isNullOrBlank()) AppVoiceActions.matchSpoken(feature) else null
    }

    private fun Bundle.spoken(vararg keys: String): String? {
        keys.forEach { key ->
            val value = getString(key)?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return null
    }
}
