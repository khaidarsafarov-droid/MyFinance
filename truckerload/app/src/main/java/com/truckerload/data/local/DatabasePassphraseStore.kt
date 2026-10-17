package com.truckerload.data.local

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A random 256-bit database passphrase. Only the AES-GCM wrapping of that
 * passphrase is written to disk. On a device the wrapping key stays inside
 * the Android Keystore.
 */
internal class DatabasePassphraseStore(
    private val wrappedFile: File,
    private val cipher: PassphraseCipher,
) {
    fun readIfPresent(): ByteArray? {
        if (!wrappedFile.exists() || wrappedFile.length() == 0L) return null
        return runCatching { cipher.unwrap(wrappedFile.readBytes()) }.getOrNull()
    }

    fun getOrCreate(): ByteArray {
        if (wrappedFile.exists() && wrappedFile.length() > 0L) {
            return readIfPresent()
                ?: error("database passphrase wrap exists but could not be unwrapped")
        }
        val fresh = ByteArray(PASSPHRASE_BYTES)
        SecureRandom().nextBytes(fresh)
        wrappedFile.parentFile?.mkdirs()
        val tmp = File(wrappedFile.path + ".tmp")
        tmp.writeBytes(cipher.wrap(fresh))
        if (!tmp.renameTo(wrappedFile)) {
            wrappedFile.writeBytes(tmp.readBytes())
            tmp.delete()
        }
        return fresh
    }

    companion object {
        const val PASSPHRASE_BYTES = 32
        private const val KEYSTORE_ALIAS = "truckerload_room_wrap"

        fun forDatabase(context: Context, databaseName: String): DatabasePassphraseStore {
            val file = File(context.filesDir, "room-passphrases/$databaseName.wrap")
            val cipher = if (isRobolectric()) {
                SoftwarePassphraseCipher.forFile(File(context.filesDir, "room-passphrases/robolectric-wrap.key"))
            } else {
                AndroidKeystorePassphraseCipher(KEYSTORE_ALIAS)
            }
            return DatabasePassphraseStore(file, cipher)
        }

        fun isRobolectric(): Boolean = Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }
}

internal interface PassphraseCipher {
    fun wrap(plain: ByteArray): ByteArray
    fun unwrap(wrapped: ByteArray): ByteArray
}

/** AES-GCM with a caller-supplied key. Tests use this to prove wrap/unwrap without Keystore. */
internal class AesGcmPassphraseCipher(
    private val key: SecretKey,
) : PassphraseCipher {
    override fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val body = cipher.doFinal(plain)
        return byteArrayOf(VERSION) + iv + body
    }

    override fun unwrap(wrapped: ByteArray): ByteArray {
        require(wrapped.size > 1 + IV_BYTES && wrapped[0] == VERSION) { "wrapped passphrase is corrupt" }
        val iv = wrapped.copyOfRange(1, 1 + IV_BYTES)
        val body = wrapped.copyOfRange(1 + IV_BYTES, wrapped.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(body)
    }

    companion object {
        private const val VERSION: Byte = 1
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

internal class AndroidKeystorePassphraseCipher(
    private val alias: String,
) : PassphraseCipher {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    override fun wrap(plain: ByteArray): ByteArray = AesGcmPassphraseCipher(secretKey()).wrap(plain)

    override fun unwrap(wrapped: ByteArray): ByteArray = AesGcmPassphraseCipher(secretKey()).unwrap(wrapped)

    private fun secretKey(): SecretKey {
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}

/** Robolectric has no Android Keystore. The wrapping key stays in the test process files. */
internal class SoftwarePassphraseCipher private constructor(
    key: SecretKey,
) : PassphraseCipher by AesGcmPassphraseCipher(key) {
    companion object {
        fun forFile(keyFile: File): SoftwarePassphraseCipher {
            val bytes = if (keyFile.exists() && keyFile.length() == 32L) {
                keyFile.readBytes()
            } else {
                ByteArray(32).also { SecureRandom().nextBytes(it) }.also { fresh ->
                    keyFile.parentFile?.mkdirs()
                    keyFile.writeBytes(fresh)
                }
            }
            return SoftwarePassphraseCipher(javax.crypto.spec.SecretKeySpec(bytes, "AES"))
        }
    }
}
