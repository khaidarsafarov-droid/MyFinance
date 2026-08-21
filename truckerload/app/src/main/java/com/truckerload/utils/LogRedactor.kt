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
}
