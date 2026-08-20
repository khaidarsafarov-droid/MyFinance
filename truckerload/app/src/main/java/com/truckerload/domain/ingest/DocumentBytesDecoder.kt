package com.truckerload.domain.ingest

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Pulls plain text from office / text files without OCR.
 * Images and scanned PDFs stay in the Android extractor.
 */
object DocumentBytesDecoder {

    fun decode(bytes: ByteArray, fileName: String? = null, mimeType: String? = null): String? {
        if (bytes.isEmpty()) return null
        val name = fileName.orEmpty().lowercase(Locale.US)
        val mime = mimeType.orEmpty().lowercase(Locale.US)
        return when {
            isPdf(bytes, name, mime) -> null
            isZipOffice(bytes) && (name.endsWith(".docx") || mime.contains("wordprocessingml")) ->
                extractZipXml(bytes, "word/document.xml", docx = true)
            isZipOffice(bytes) && (name.endsWith(".xlsx") || mime.contains("spreadsheetml")) ->
                extractXlsx(bytes)
            isHtml(name, mime, bytes) -> stripHtml(decodeText(bytes))
            isPlainish(name, mime) -> decodeText(bytes).trim().ifBlank { null }
            looksLikeText(bytes) -> decodeText(bytes).trim().ifBlank { null }
            else -> null
        }
    }

    fun isPdf(bytes: ByteArray, fileName: String, mimeType: String): Boolean {
        if (fileName.endsWith(".pdf") || mimeType.contains("pdf")) return true
        return bytes.size >= 5 && bytes.decodeToString(0, 5) == "%PDF-"
    }

    fun isImage(bytes: ByteArray, fileName: String, mimeType: String): Boolean {
        if (mimeType.startsWith("image/")) return true
        val n = fileName.lowercase(Locale.US)
        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") ||
            n.endsWith(".webp") || n.endsWith(".heic") || n.endsWith(".gif")
        ) {
            return true
        }
        if (bytes.size < 12) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        if (b0 == 0xFF && b1 == 0xD8) return true
        if (b0 == 0x89 && bytes.copyOfRange(0, 4).contentEquals(PNG_SIG)) return true
        val head = bytes.decodeToString(0, minOf(12, bytes.size))
        return head.startsWith("RIFF") && head.contains("WEBP")
    }

    private fun isZipOffice(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    private fun isHtml(name: String, mime: String, bytes: ByteArray): Boolean {
        if (name.endsWith(".html") || name.endsWith(".htm") || mime.contains("html")) return true
        val sample = decodeText(bytes.copyOf(minOf(bytes.size, 400))).lowercase(Locale.US)
        return sample.contains("<html") || sample.contains("<!doctype html")
    }

    private fun isPlainish(name: String, mime: String): Boolean =
        name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".json") ||
            name.endsWith(".xml") || name.endsWith(".log") ||
            mime.startsWith("text/") || mime == "application/json" || mime == "application/xml"

    private fun looksLikeText(bytes: ByteArray): Boolean {
        val sample = bytes.copyOf(minOf(bytes.size, 2048))
        if (sample.isEmpty()) return false
        val printable = sample.count { b ->
            val c = b.toInt() and 0xFF
            c == 0x09 || c == 0x0A || c == 0x0D || c in 0x20..0x7E || c >= 0x80
        }
        return printable * 10 >= sample.size * 9
    }

    internal fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        val utf8 = String(bytes, Charsets.UTF_8)
        val bad = utf8.count { it == '\uFFFD' }
        if (bad > 3 && bad * 20 > utf8.length) {
            return String(bytes, Charset.forName("windows-1251"))
        }
        return utf8
    }

    internal fun stripHtml(raw: String): String {
        return raw
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("(?i)</tr>"), "\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("\\s+\n"), "\n")
            .replace(Regex("[ \\t]{2,}"), " ")
            .trim()
    }

    private fun extractZipXml(bytes: ByteArray, path: String, docx: Boolean): String? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == path) {
                    val xml = decodeText(zip.readBytes())
                    val withBreaks = if (docx) {
                        xml.replace(Regex("</w:p>"), "\n")
                            .replace(Regex("<w:tab[^/]*/>"), "\t")
                    } else {
                        xml
                    }
                    return stripHtml(withBreaks).ifBlank { null }
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun extractXlsx(bytes: ByteArray): String? {
        val shared = extractZipXml(bytes, "xl/sharedStrings.xml", docx = false).orEmpty()
        val sheet = extractZipXml(bytes, "xl/worksheets/sheet1.xml", docx = false).orEmpty()
        val combined = "$shared\n$sheet".trim()
        return combined.ifBlank { null }
    }

    private val PNG_SIG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
}
