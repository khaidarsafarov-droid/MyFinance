package com.truckerload.data.sync

import com.truckerload.contract.MediaKind
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class ValidatedMediaFile(
    val file: File,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
)

class MediaValidationException(val code: String) : IllegalArgumentException(code)

object MediaFilePolicy {
    const val MAX_BYTES = 25L * 1024 * 1024

    fun validateUpload(file: File, kind: MediaKind): ValidatedMediaFile {
        if (!file.isFile) throw MediaValidationException("missing_file")
        val size = file.length()
        if (size !in 1..MAX_BYTES) throw MediaValidationException("invalid_size")
        val contentType = FileInputStream(file).use { input ->
            val prefix = ByteArray(16)
            val count = input.read(prefix)
            detectContentType(prefix, count)
        } ?: throw MediaValidationException("invalid_content")
        if (!supports(kind, contentType)) throw MediaValidationException("invalid_content")
        return ValidatedMediaFile(file, contentType, size, sha256(file))
    }

    fun validateRemote(
        kind: MediaKind,
        clientId: String,
        fileName: String,
        contentType: String,
        sizeBytes: Long,
        checksum: String?,
    ) {
        if (!validClientId(clientId)) throw MediaValidationException("invalid_client_id")
        if (fileName.substringAfterLast('/').substringAfterLast('\\').length !in 1..180) {
            throw MediaValidationException("invalid_file_name")
        }
        val normalized = contentType.substringBefore(';').trim().lowercase()
        if (!supports(kind, normalized)) throw MediaValidationException("invalid_content")
        if (sizeBytes !in 1..MAX_BYTES) throw MediaValidationException("invalid_size")
        if (checksum != null && !checksum.matches(Regex("[a-fA-F0-9]{64}"))) {
            throw MediaValidationException("invalid_checksum")
        }
    }

    fun destinationName(kind: MediaKind, clientId: String, contentType: String): String {
        if (!validClientId(clientId)) throw MediaValidationException("invalid_client_id")
        val extension = when (contentType.substringBefore(';').trim().lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> when (kind) {
                MediaKind.PHOTO -> "jpg"
                MediaKind.SCAN -> "pdf"
            }
        }
        val safeId = clientId.map { if (it.isLetterOrDigit() || it in "-_.") it else '_' }.joinToString("")
        return "$safeId.$extension"
    }

    fun verifyChecksum(file: File, expected: String?) {
        if (expected == null) return
        val actual = sha256(file).encodeToByteArray()
        if (!MessageDigest.isEqual(actual, expected.lowercase().encodeToByteArray())) {
            throw MediaValidationException("checksum_mismatch")
        }
    }

    fun contentMatches(file: File, expectedContentType: String) {
        val detected = FileInputStream(file).use { input ->
            val prefix = ByteArray(16)
            val count = input.read(prefix)
            detectContentType(prefix, count)
        }
        if (detected != expectedContentType.substringBefore(';').trim().lowercase()) {
            throw MediaValidationException("invalid_content")
        }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun validClientId(value: String): Boolean =
        value.length in 1..128 && value.none { it.isISOControl() || it == '/' || it == '\\' }

    private fun supports(kind: MediaKind, contentType: String): Boolean = when (kind) {
        MediaKind.PHOTO -> contentType in setOf("image/jpeg", "image/png", "image/webp")
        MediaKind.SCAN -> contentType in setOf("application/pdf", "image/jpeg", "image/png")
    }

    private fun detectContentType(bytes: ByteArray, count: Int): String? {
        if (count >= 4 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }
        if (count >= 8 &&
            bytes.sliceArray(0..7).contentEquals(
                byteArrayOf(
                    0x89.toByte(), 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A,
                ),
            )
        ) {
            return "image/png"
        }
        if (count >= 12 &&
            bytes.copyOfRange(0, 4).decodeToString() == "RIFF" &&
            bytes.copyOfRange(8, 12).decodeToString() == "WEBP"
        ) {
            return "image/webp"
        }
        if (count >= 5 && bytes.copyOfRange(0, 5).decodeToString() == "%PDF-") {
            return "application/pdf"
        }
        return null
    }
}
