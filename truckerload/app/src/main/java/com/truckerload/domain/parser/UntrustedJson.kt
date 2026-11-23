package com.truckerload.domain.parser

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses JSON that arrived from a share sheet, a chat update, or a Telegram
 * Desktop export. org.json before 20260719 could stall on one huge number
 * (CVE-2026-59171). The library is patched. A single share or chat value stays
 * under [MAX_CHARS]. A full result.json export may be many megabytes; that path
 * uses [MAX_EXPORT_CHARS] and caps each message with [MAX_MESSAGE_CHARS].
 */
object UntrustedJson {

    const val MAX_CHARS = 256 * 1024
    const val MAX_MESSAGE_CHARS = 32 * 1024
    const val MAX_EXPORT_CHARS = 16 * 1024 * 1024

    fun objectOrNull(raw: String, maxChars: Int = MAX_CHARS): JSONObject? {
        val text = raw.trim()
        if (text.length > maxChars || !text.startsWith("{")) return null
        return runCatching { JSONObject(text) }.getOrNull()
    }

    fun arrayOrNull(raw: String, maxChars: Int = MAX_CHARS): JSONArray? {
        val text = raw.trim()
        if (text.length > maxChars || !text.startsWith("[")) return null
        return runCatching { JSONArray(text) }.getOrNull()
    }

    fun valueOrNull(raw: String, maxChars: Int = MAX_CHARS): Any? {
        val text = raw.trim()
        if (text.length > maxChars) return null
        return when {
            text.startsWith("{") -> objectOrNull(text, maxChars)
            text.startsWith("[") -> arrayOrNull(text, maxChars)
            else -> null
        }
    }

    fun capMessage(text: String): String = text.take(MAX_MESSAGE_CHARS)
}
