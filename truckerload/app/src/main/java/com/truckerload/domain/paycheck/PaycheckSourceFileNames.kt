package com.truckerload.domain.paycheck

/** Display-name sanitizing and MIME guesses for stored settlement originals. */
object PaycheckSourceFileNames {

    private val unsafeChars = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    fun sanitize(raw: String?): String {
        val trimmed = raw.orEmpty().substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = unsafeChars.replace(trimmed, "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
        val withFallback = cleaned.ifBlank { "settlement" }
        return withFallback.take(80)
    }

    fun mimeType(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "gif" -> "image/gif"
        "txt" -> "text/plain"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        else -> "application/octet-stream"
    }
}
