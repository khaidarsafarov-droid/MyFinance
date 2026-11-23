package com.truckerload.domain.share

import java.security.MessageDigest
import java.util.Locale

/**
 * Caps and checks for bytes that arrived from another app.
 * The sender's file name is never used as a path.
 */
object SharePayloadPolicy {

    const val MAX_FILES = 8
    const val MAX_FILE_BYTES = 20L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 40L * 1024 * 1024

    enum class Sniffed {
        PDF,
        JPEG,
        PNG,
        GIF,
        WEBP,
        HEIC,
        TEXT,
        UNKNOWN,
    }

    fun isAllowedMime(mime: String?): Boolean {
        val type = mime?.substringBefore(';')?.trim()?.lowercase(Locale.US).orEmpty()
        return type == "text/plain" || type == "application/pdf" || type.startsWith("image/")
    }

    fun extensionFor(mime: String?): String = when (
        mime?.substringBefore(';')?.trim()?.lowercase(Locale.US).orEmpty()
    ) {
        "application/pdf" -> "pdf"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/heic", "image/heif" -> "heic"
        "text/plain" -> "txt"
        else -> if (mime.orEmpty().startsWith("image/")) "jpg" else "bin"
    }

    /** Path segment we invent. The argument is only the mime, never the sender's name. */
    fun storageStem(index: Int): String = "share-$index"

    /**
     * Label stored next to the scan. Slashes and traversal are stripped.
     * Empty or hostile names become "shared".
     */
    fun displayLabel(raw: String?): String {
        val base = raw
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            .orEmpty()
        val cleaned = base
            .replace(Regex("""[^A-Za-z0-9._ -]"""), "")
            .trim('.', ' ', '-')
            .take(60)
            .trim()
        if (cleaned.isBlank() || cleaned == "." || cleaned == "..") return "shared"
        return cleaned
    }

    fun sniff(header: ByteArray): Sniffed {
        if (header.size >= 5 && header.copyOfRange(0, 5).contentEquals(PDF)) return Sniffed.PDF
        if (header.size >= 3 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() &&
            header[2] == 0xFF.toByte()
        ) {
            return Sniffed.JPEG
        }
        if (header.size >= 8 && header.copyOfRange(0, 8).contentEquals(PNG)) return Sniffed.PNG
        if (header.size >= 6) {
            val gif = header.copyOfRange(0, 6).toString(Charsets.US_ASCII)
            if (gif == "GIF87a" || gif == "GIF89a") return Sniffed.GIF
        }
        if (header.size >= 12 &&
            header.copyOfRange(0, 4).contentEquals(RIFF) &&
            header.copyOfRange(8, 12).contentEquals(WEBP)
        ) {
            return Sniffed.WEBP
        }
        if (isHeic(header)) return Sniffed.HEIC
        if (header.isEmpty()) return Sniffed.UNKNOWN
        if (header.any { it == 0.toByte() }) return Sniffed.UNKNOWN
        return Sniffed.TEXT
    }

    fun mimeAgrees(declared: String?, sniffed: Sniffed): Boolean {
        val mime = declared?.substringBefore(';')?.trim()?.lowercase(Locale.US).orEmpty()
        if (!isAllowedMime(mime)) return false
        return when (sniffed) {
            Sniffed.PDF -> mime == "application/pdf"
            Sniffed.JPEG, Sniffed.PNG, Sniffed.GIF, Sniffed.WEBP, Sniffed.HEIC ->
                mime.startsWith("image/")
            Sniffed.TEXT -> mime == "text/plain"
            Sniffed.UNKNOWN -> false
        }
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun isHeic(header: ByteArray): Boolean {
        if (header.size < 12) return false
        if (!header.copyOfRange(4, 8).contentEquals(FTYP)) return false
        val brand = header.copyOfRange(8, 12).toString(Charsets.US_ASCII).lowercase(Locale.US)
        return brand in HEIC_BRANDS
    }

    private val PDF = "%PDF-".toByteArray(Charsets.US_ASCII)
    private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val RIFF = "RIFF".toByteArray(Charsets.US_ASCII)
    private val WEBP = "WEBP".toByteArray(Charsets.US_ASCII)
    private val FTYP = "ftyp".toByteArray(Charsets.US_ASCII)
    private val HEIC_BRANDS = setOf("heic", "heix", "heif", "mif1", "msf1")
}
