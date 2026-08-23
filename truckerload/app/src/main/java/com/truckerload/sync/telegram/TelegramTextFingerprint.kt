package com.truckerload.sync.telegram

import java.security.MessageDigest

/** Stable content fingerprint for diesel/paycheck dedupe (device + server paths). */
object TelegramTextFingerprint {
    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** Fingerprint for fuel rows; [salt] distinguishes DEF vs diesel when text overlaps. */
    fun dieselFingerprint(rawText: String, salt: String? = null): String {
        val payload = if (salt.isNullOrBlank()) rawText else "$rawText|$salt"
        return sha256Hex(payload)
    }
}
