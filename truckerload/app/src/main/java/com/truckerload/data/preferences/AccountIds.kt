package com.truckerload.data.preferences

import com.truckerload.utils.normalizeKey
import java.security.MessageDigest

/**
 * Identifier of the single device profile. New installs use [LOCAL_DEV]; installs upgraded
 * from cloud-era builds keep their persisted `google_…` / `local_…` / UUID id, because the
 * Room file and encrypted preference files are named after it.
 */
object AccountIds {
    const val LOCAL_DEV = "local_dev"

    fun fromEmail(email: String): String {
        val normalized = email.normalizeKey()
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

    /** Safe fragment for Room database file names. */
    fun sanitizeFilePart(userId: String): String =
        userId.trim()
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(80)
            .ifBlank { "unknown" }
}
