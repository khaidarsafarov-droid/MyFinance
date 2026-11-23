package com.truckerload.domain.parser

/**
 * One inbound load payload. The subtype is source metadata (where the text came from).
 * [LoadParsingEngine.parse] ranks every parser the same way for each subtype, so a rate
 * confirmation pasted in chat, received on Telegram, or read out of a PDF/image agrees.
 */
sealed class ParseInput {
    abstract val text: String
    abstract val referenceMillis: Long
    abstract val fileName: String?

    /** Add-load paste and any other plain text that is not tied to a file or a chat message. */
    data class PlainText(
        override val text: String,
        override val referenceMillis: Long = System.currentTimeMillis(),
        override val fileName: String? = null,
    ) : ParseInput()

    /** Telegram chat text. [messageDateSeconds] is the Telegram message date when the update has one. */
    data class TelegramMessage(
        override val text: String,
        val messageDateSeconds: Long? = null,
        override val referenceMillis: Long = messageDateSeconds?.times(1000) ?: System.currentTimeMillis(),
        override val fileName: String? = null,
    ) : ParseInput()

    /**
     * Text extracted from a PDF or image (share sheet, Telegram document, OCR).
     * [fileName] is the original file name when the caller has one.
     */
    data class ExtractedDocument(
        override val text: String,
        override val fileName: String? = null,
        override val referenceMillis: Long = System.currentTimeMillis(),
        val messageDateSeconds: Long? = null,
    ) : ParseInput()
}
