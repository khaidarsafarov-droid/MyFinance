package com.truckerload.utils

/**
 * Redacts secrets from logs / error strings (Telegram bot tokens, JWTs, API keys).
 */
object LogRedactor {
    private val botTokenInUrl = Regex("""(/bot)([0-9]+:[A-Za-z0-9_-]+)""")
    private val jwtLike = Regex("""\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b""")
    private val bearer = Regex("""(?i)(Bearer\s+)([A-Za-z0-9._\-+=/]+)""")
    private val anonKeyHint = Regex("""(?i)(supabase[_-]?anon[_-]?key\s*[=:]\s*)(\S+)""")
    private val googleApiKey = Regex("""AIza[0-9A-Za-z_\-]{10,}""")

    fun redact(raw: String?): String {
        if (raw.isNullOrBlank()) return raw.orEmpty()
        return raw
            .replace(botTokenInUrl) { "${it.groupValues[1]}***" }
            .replace(jwtLike, "***jwt***")
            .replace(bearer) { "${it.groupValues[1]}***" }
            .replace(anonKeyHint) { "${it.groupValues[1]}***" }
            .replace(googleApiKey, "***gkey***")
    }

    /**
     * A loggable copy of [error] whose message and cause chain no longer contain
     * bot tokens, bearer tokens, or Google API keys. The original stack is kept.
     */
    fun redactThrowable(error: Throwable): Throwable {
        val message = redact(collectMessages(error).ifBlank { error.javaClass.simpleName })
        val safe = RedactedLogException(message)
        safe.stackTrace = error.stackTrace
        return safe
    }

    private fun collectMessages(error: Throwable): String {
        val parts = ArrayList<String>(4)
        var current: Throwable? = error
        var guard = 0
        while (current != null && guard++ < 8) {
            current.message?.let { parts.add(it) }
            current = current.cause
        }
        return parts.joinToString(" | ")
    }

    private class RedactedLogException(message: String) : Exception(message)
}
