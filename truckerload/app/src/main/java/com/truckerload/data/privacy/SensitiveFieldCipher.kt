package com.truckerload.data.privacy

import android.content.Context
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.SecurePreferences
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM for sensitive columns (CDL number / document URL).
 *
 * Ciphertext is stored in Room; the data key lives in [SecurePreferences]
 * (EncryptedSharedPreferences + Android Keystore when available).
 *
 * Wire format: `v1:` + Base64(iv || ciphertext+tag). Legacy SQL copies use `plain:`.
 */
interface SensitiveFieldCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(stored: String): String
    fun wipe()
}

class AesGcmSensitiveFieldCipher(
    private val keyBytes: ByteArray,
    private val onWipe: () -> Unit = {},
) : SensitiveFieldCipher {

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
        when {
            stored.startsWith(PREFIX_PLAIN) -> return stored.removePrefix(PREFIX_PLAIN)
            !stored.startsWith(PREFIX_V1) -> return stored
        }
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

    override fun wipe() {
        onWipe()
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

        fun forUser(context: Context, userId: String): SensitiveFieldCipher {
            val id = AccountIds.sanitizeFilePart(userId)
            val prefs = SecurePreferences.open(context.applicationContext, PREFS_PREFIX + id)
            val existing = prefs.getString(KEY_AES, null)
            val key = if (existing.isNullOrBlank()) {
                val generated = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
                prefs.edit { putString(KEY_AES, Base64.getEncoder().encodeToString(generated)) }
                generated
            } else {
                Base64.getDecoder().decode(existing)
            }
            return AesGcmSensitiveFieldCipher(key) {
                prefs.edit { clear() }
            }
        }

        fun wrapPlaintextForMigration(plaintext: String): String {
            if (plaintext.isBlank()) return ""
            return PREFIX_PLAIN + plaintext
        }
    }
}
