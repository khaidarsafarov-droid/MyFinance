package com.truckerload.data.auth

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Client-side password rules + at-rest hashing for offline email login.
 * Plaintext is never persisted — only a PBKDF2-SHA256 verifier string.
 */
object PasswordPolicy {
    const val MIN_LENGTH = 8
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val PREFIX = "pbkdf2$"

    data class Validation(
        val ok: Boolean,
        /** String resource id when [ok] is false; 0 when ok. */
        val errorResId: Int = 0,
    )

    fun validate(password: String): Validation = when {
        password.length < MIN_LENGTH ->
            Validation(false, com.truckerload.R.string.auth_error_password_short)
        password.none { it.isDigit() } ->
            Validation(false, com.truckerload.R.string.auth_error_password_needs_digit)
        password.none { it.isUpperCase() } ->
            Validation(false, com.truckerload.R.string.auth_error_password_needs_upper)
        else -> Validation(true)
    }

    fun isHashed(stored: String): Boolean = stored.startsWith(PREFIX)

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password.toCharArray(), salt, ITERATIONS)
        return buildString {
            append(PREFIX)
            append(ITERATIONS)
            append('$')
            append(Base64.getEncoder().encodeToString(salt))
            append('$')
            append(Base64.getEncoder().encodeToString(hash))
        }
    }

    fun matches(password: String, stored: String): Boolean {
        if (password.isBlank() || stored.isBlank()) return false
        if (!isHashed(stored)) {
            // Legacy plaintext (pre-hash installs) — constant-time-ish compare.
            return stored == password
        }
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        // FIX: cap attacker-controlled iteration count to prevent CPU hang on poisoned verifiers
        if (iterations !in 10_000..ITERATIONS) return false
        val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull() ?: return false
        val actual = pbkdf2(password.toCharArray(), salt, iterations)
        return constantTimeEquals(expected, actual)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
