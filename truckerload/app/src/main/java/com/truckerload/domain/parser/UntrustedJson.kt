package com.truckerload.domain.parser

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses JSON that arrived from a share sheet or a chat.
 * org.json before 20260719 could stall on one huge number (CVE-2026-59171).
 * The library is patched, and this cap still refuses oversized payloads
 * before the parser runs.
 */
object UntrustedJson {

    const val MAX_CHARS = 256 * 1024

    fun objectOrNull(raw: String): JSONObject? {
        val text = raw.trim()
        if (text.length > MAX_CHARS || !text.startsWith("{")) return null
        return runCatching { JSONObject(text) }.getOrNull()
    }

    fun arrayOrNull(raw: String): JSONArray? {
        val text = raw.trim()
        if (text.length > MAX_CHARS || !text.startsWith("[")) return null
        return runCatching { JSONArray(text) }.getOrNull()
    }

    fun valueOrNull(raw: String): Any? {
        val text = raw.trim()
        if (text.length > MAX_CHARS) return null
        return when {
            text.startsWith("{") -> objectOrNull(text)
            text.startsWith("[") -> arrayOrNull(text)
            else -> null
        }
    }
}
