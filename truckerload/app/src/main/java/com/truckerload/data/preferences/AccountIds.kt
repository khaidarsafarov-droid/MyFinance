package com.truckerload.data.preferences

import java.security.MessageDigest

/**
 * Stable account identifiers for multi-user local isolation.
 * Priority: Supabase UUID → Google `sub` → email hash → LOCAL_DEV.
 *
 * Isolation note: Room DB file name and preference/DataStore file names are keyed by
 * [sanitizeFilePart] of the resolved account id, so two device logins with different
 * emails / Google accounts do not share loads/settings.
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

    /** Stable Room / prefs id derived from Google OpenID `sub`. */
    fun fromGoogleSub(googleSub: String): String {
        val sub = googleSub.trim()
        require(sub.isNotBlank()) { "google sub required" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sub.toByteArray(Charsets.UTF_8))
        val hex = digest.take(16).joinToString("") { b -> "%02x".format(b) }
        return "google_$hex"
    }

    /**
     * @return account id, or null when no usable identity is available.
     */
    fun resolveOrNull(
        supabaseUserId: String?,
        email: String?,
        googleSub: String? = null,
    ): String? {
        val remote = supabaseUserId?.trim().orEmpty()
        if (remote.isNotBlank()) return remote
        val sub = googleSub?.trim().orEmpty()
        if (sub.isNotBlank()) return fromGoogleSub(sub)
        val mail = email?.trim().orEmpty()
        if (mail.isBlank()) return null
        return fromEmail(mail)
    }

    fun resolve(supabaseUserId: String?, email: String, googleSub: String? = null): String =
        resolveOrNull(supabaseUserId, email, googleSub)
            ?: error("Cannot resolve account id without Supabase user id, Google sub, or email")

    /** Safe fragment for Room database file names. */
    fun sanitizeFilePart(userId: String): String =
        userId.trim()
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(80)
            .ifBlank { "unknown" }
}
