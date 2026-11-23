package com.truckerload.data.privacy

import android.content.Context
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.SecurePreferences
import com.truckerload.data.preferences.SecureStorageUnavailableException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM for sensitive columns (CDL number / document URL).
 *
 * Ciphertext is stored in Room. The data key lives only in encrypted preferences.
 * If the Android Keystore is unavailable, the key is not written to a plaintext file.
 *
 * Wire format: `v1:` + Base64(iv || ciphertext+tag).
 * Older rows may still say `plain:` or raw text. The first read rewrites those to
 * `v1:` and then [decrypt] stops accepting plaintext.
 */
interface SensitiveFieldCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(stored: String): String
    fun wipe()

    /** True when this cipher can rewrite legacy plaintext and then refuse it. */
    fun supportsLegacyMigration(): Boolean = false

    fun isLegacyRetired(): Boolean = false

    /**
     * Returns a `v1:` replacement for a legacy value, or null when [stored] is
     * already encrypted, blank, or migration is finished.
     */
    fun reencryptLegacy(stored: String?): String? = null

    fun retireLegacyPlaintext() {}
}

class AesGcmSensitiveFieldCipher(
    private val keyBytes: ByteArray,
    private val onWipe: () -> Unit = {},
    private val legacyRetired: (() -> Boolean)? = null,
    private val markLegacyRetired: (() -> Unit)? = null,
) : SensitiveFieldCipher {

    private var retiredInMemory: Boolean = false

    override fun supportsLegacyMigration(): Boolean = true

    override fun isLegacyRetired(): Boolean = legacyRetired?.invoke() ?: retiredInMemory

    override fun retireLegacyPlaintext() {
        val mark = markLegacyRetired
        if (mark != null) mark() else retiredInMemory = true
    }

    override fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return PREFIX_V1 + Base64.getEncoder().encodeToString(iv + encrypted)
    }

    override fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        if (stored.startsWith(PREFIX_V1)) return decryptV1(stored)
        if (isLegacyRetired()) return ""
        return legacyCleartext(stored)
    }

    override fun reencryptLegacy(stored: String?): String? {
        if (stored.isNullOrBlank() || stored.startsWith(PREFIX_V1) || isLegacyRetired()) return null
        val plain = legacyCleartext(stored)
        if (plain.isBlank()) return null
        return encrypt(plain)
    }

    override fun wipe() {
        onWipe()
    }

    private fun decryptV1(stored: String): String {
        val raw = runCatching {
            Base64.getDecoder().decode(stored.removePrefix(PREFIX_V1))
        }.getOrNull() ?: return ""
        if (raw.size <= IV_BYTES) return ""
        val iv = raw.copyOfRange(0, IV_BYTES)
        val body = raw.copyOfRange(IV_BYTES, raw.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(body), Charsets.UTF_8)
    }

    private fun legacyCleartext(stored: String): String = when {
        stored.startsWith(PREFIX_PLAIN) -> stored.removePrefix(PREFIX_PLAIN)
        stored.startsWith(PREFIX_V1) -> ""
        else -> stored
    }

    companion object {
        const val PREFIX_V1 = "v1:"
        const val PREFIX_PLAIN = "plain:"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val KEY_BYTES = 32
        private const val PREFS_PREFIX = "truckerload_sensitive_"
        private const val KEY_AES = "aes_gcm_key"
        private const val KEY_LEGACY_RETIRED = "cdl_plaintext_retired"

        fun forUser(context: Context, userId: String): SensitiveFieldCipher {
            val id = AccountIds.sanitizeFilePart(userId)
            val prefs = try {
                SecurePreferences.open(
                    context.applicationContext,
                    PREFS_PREFIX + id,
                    secrets = true,
                )
            } catch (_: SecureStorageUnavailableException) {
                return UnavailableSensitiveFieldCipher
            }
            val existing = prefs.getString(KEY_AES, null)
            val key = if (existing.isNullOrBlank()) {
                val generated = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
                prefs.edit { putString(KEY_AES, Base64.getEncoder().encodeToString(generated)) }
                generated
            } else {
                Base64.getDecoder().decode(existing)
            }
            return AesGcmSensitiveFieldCipher(
                keyBytes = key,
                onWipe = { prefs.edit { clear() } },
                legacyRetired = { prefs.getBoolean(KEY_LEGACY_RETIRED, false) },
                markLegacyRetired = {
                    prefs.edit { putBoolean(KEY_LEGACY_RETIRED, true) }
                },
            )
        }

        fun wrapPlaintextForMigration(plaintext: String): String {
            if (plaintext.isBlank()) return ""
            return PREFIX_PLAIN + plaintext
        }
    }
}

/** Used when the Keystore cannot hold the CDL key. Does not persist a new key. */
private object UnavailableSensitiveFieldCipher : SensitiveFieldCipher {
    override fun encrypt(plaintext: String): String {
        error("Secure storage unavailable; refusing to persist a sensitive field")
    }

    override fun decrypt(stored: String): String = ""

    override fun wipe() = Unit
}
