package com.truckerload.data.privacy

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption for local backup files, keyed in Android Keystore.
 */
object BackupCrypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "truckerload_backup_aes"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private const val MAGIC = "TLB1"

    fun encrypt(plainUtf8: String): ByteArray {
        ensureKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainUtf8.toByteArray(Charsets.UTF_8))
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        return ByteBuffer.allocate(magic.size + 1 + iv.size + cipherBytes.size)
            .put(magic)
            .put(IV_BYTES.toByte())
            .put(iv)
            .put(cipherBytes)
            .array()
    }

    fun encrypt(context: Context, plainUtf8: String): ByteArray = encrypt(plainUtf8)

    fun decrypt(bytes: ByteArray): String {
        if (bytes.size < MAGIC.length + 1 + IV_BYTES) {
            // Legacy plaintext JSON backups.
            return String(bytes, Charsets.UTF_8)
        }
        val magic = String(bytes, 0, MAGIC.length, Charsets.US_ASCII)
        if (magic != MAGIC) {
            return String(bytes, Charsets.UTF_8)
        }
        ensureKey()
        val ivLen = bytes[MAGIC.length].toInt() and 0xFF
        require(ivLen == IV_BYTES) { "bad iv" }
        val ivStart = MAGIC.length + 1
        val iv = bytes.copyOfRange(ivStart, ivStart + ivLen)
        val cipherBytes = bytes.copyOfRange(ivStart + ivLen, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }

    fun decrypt(context: Context, bytes: ByteArray): String = decrypt(bytes)

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.length && String(bytes, 0, MAGIC.length, Charsets.US_ASCII) == MAGIC

    fun hashPassword(password: String, saltBase64: String? = null): Pair<String, String> {
        val salt = if (saltBase64.isNullOrBlank()) {
            val raw = ByteArray(16)
            SecureRandom().nextBytes(raw)
            Base64.encodeToString(raw, Base64.NO_WRAP)
        } else {
            saltBase64
        }
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltBytes)
        digest.update(password.toByteArray(Charsets.UTF_8))
        val hash = Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
        return hash to salt
    }

    fun verifyPassword(password: String, storedHash: String, salt: String): Boolean {
        val (computed, _) = hashPassword(password, salt)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8),
        )
    }

    private fun ensureKey() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) return
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun getKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }
}
