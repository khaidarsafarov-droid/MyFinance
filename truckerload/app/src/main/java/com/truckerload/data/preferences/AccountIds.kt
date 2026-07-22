package com.truckerload.data.preferences

import java.security.MessageDigest

/**
 * Stable account identifiers for multi-user local isolation.
 * Supabase users keep their UUID; offline / Google-without-Supabase use a hash of email.
 */
object AccountIds {
    /** Single-device offline mode ([com.truckerload.BuildConfig.LOCAL_ONLY_MODE]). */
    const val LOCAL_DEV = "local_dev"

    fun fromEmail(email: String): String {
        val normalized = email.trim().lowercase()
        require(normalized.isNotBlank()) { "email required for local account id" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        val hex = digest.take(16).joinToString("") { b -> "%02x".format(b) }
        return "local_$hex"
    }

    /**
     * @return account id, or null when neither Supabase id nor a usable email is available.
     */
    fun resolveOrNull(supabaseUserId: String?, email: String?): String? {
        val remote = supabaseUserId?.trim().orEmpty()
        if (remote.isNotBlank()) return remote
        val mail = email?.trim().orEmpty()
        if (mail.isBlank()) return null
        return fromEmail(mail)
    }

    fun resolve(supabaseUserId: String?, email: String): String =
        resolveOrNull(supabaseUserId, email)
            ?: error("Cannot resolve account id without Supabase user id or email")

    /** Safe fragment for Room database file names. */
    fun sanitizeFilePart(userId: String): String =
        userId.trim()
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(80)
            .ifBlank { "unknown" }
}
