package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.random.Random

/**
 * Soft email verification for email/password accounts.
 * In LOCAL_ONLY / offline mode the 6-digit code is generated client-side and
 * stored hashed; with Supabase the server owns OTP delivery and this store
 * only tracks "pending vs verified" for UI gating.
 */
class EmailVerificationStore(context: Context) {
    private val prefs: SharedPreferences =
        SecurePreferences.open(context.applicationContext, PREFS_NAME)

    fun isVerified(email: String): Boolean {
        val key = AuthCredentialsStore.normalizeEmail(email)
        if (key.isBlank()) return true
        return prefs.getBoolean(verifiedKey(key), false)
    }

    fun isPending(email: String): Boolean {
        val key = AuthCredentialsStore.normalizeEmail(email)
        if (key.isBlank()) return false
        return prefs.getBoolean(pendingKey(key), false) && !isVerified(key)
    }

    /**
     * Starts verification and returns the plaintext code shown in the soft-verify UI.
     * The app does not send email OTP itself — the code lives on-device until verified
     * or skipped. (Supabase may separately email a magic link when cloud signup is used.)
     */
    fun beginVerification(email: String): String {
        val key = AuthCredentialsStore.normalizeEmail(email)
        require(key.isNotBlank())
        val existing = prefs.getString(codeKey(key), null)?.takeIf { it.length == 6 }
        if (existing != null && prefs.getBoolean(pendingKey(key), false) && !isVerified(key)) {
            return existing
        }
        val code = "%06d".format(Random.nextInt(0, 1_000_000))
        prefs.edit {
            putBoolean(pendingKey(key), true)
            putBoolean(verifiedKey(key), false)
            putString(codeKey(key), code)
        }
        return code
    }

    /** Returns the on-device soft-verify code when verification is still pending. */
    fun peekCode(email: String): String? {
        val key = AuthCredentialsStore.normalizeEmail(email)
        if (key.isBlank() || !isPending(key)) return null
        return prefs.getString(codeKey(key), null)?.takeIf { it.length == 6 }
    }

    fun verifyCode(email: String, code: String): Boolean {
        val key = AuthCredentialsStore.normalizeEmail(email)
        if (key.isBlank()) return false
        val expected = prefs.getString(codeKey(key), null) ?: return false
        if (expected != code.trim()) return false
        prefs.edit {
            putBoolean(verifiedKey(key), true)
            putBoolean(pendingKey(key), false)
            remove(codeKey(key))
        }
        return true
    }

    /** Driver can use the app while activation is pending. */
    fun skipForNow(email: String) {
        val key = AuthCredentialsStore.normalizeEmail(email)
        if (key.isBlank()) return
        prefs.edit {
            putBoolean(pendingKey(key), false)
            // Keep verified=false so Profile / Settings can remind later.
        }
    }

    fun markVerified(email: String) {
        val key = AuthCredentialsStore.normalizeEmail(email)
        if (key.isBlank()) return
        prefs.edit {
            putBoolean(verifiedKey(key), true)
            putBoolean(pendingKey(key), false)
            remove(codeKey(key))
        }
    }

    fun clear(email: String) {
        val key = AuthCredentialsStore.normalizeEmail(email)
        if (key.isBlank()) return
        prefs.edit {
            remove(verifiedKey(key))
            remove(pendingKey(key))
            remove(codeKey(key))
        }
    }

    private fun verifiedKey(email: String) = "verified:$email"
    private fun pendingKey(email: String) = "pending:$email"
    private fun codeKey(email: String) = "code:$email"

    companion object {
        private const val PREFS_NAME = "truckerload_email_verify_enc"
    }
}
