package com.truckerload.data.repository

import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult
import com.truckerload.domain.parser.AmazonRelayParseResult
import com.truckerload.domain.parser.MessageParseService

/**
 * Facade for message parsing — no external AI providers.
 */
class AiRepository(
    private val messageParseService: MessageParseService = MessageParseService(),
) {

    suspend fun extractTextFromImage(imageBytes: ByteArray, mimeType: String): Result<String> =
        Result.failure(UnsupportedOperationException(OCR_IMAGE_DISABLED_CODE))

    companion object {
        /** Callers should map this to [com.truckerload.R.string.ocr_image_disabled]. */
        const val OCR_IMAGE_DISABLED_CODE = "OCR_IMAGE_DISABLED"
    }

    suspend fun parseLoadFromMessage(rawMessage: String): Result<Load> =
        messageParseService.parseLoadFromMessage(rawMessage)

    suspend fun parseLoadFromUserInput(rawMessage: String, fileName: String? = null): Result<Load> =
        messageParseService.parseLoadFromUserInput(rawMessage, fileName = fileName)

    fun extractLoadFields(rawMessage: String, fileName: String? = null) =
        messageParseService.extractLoadFields(rawMessage, fileName = fileName)

    suspend fun parseAmazonRelayFromMessage(rawMessage: String): Result<AmazonRelayParseResult> =
        messageParseService.parseAmazonRelayFromMessage(rawMessage)

    suspend fun parseLoadsFromMessage(rawMessage: String): Result<List<Load>> =
        messageParseService.parseLoadsFromMessage(rawMessage)

    suspend fun parsePaycheckFromText(text: String): Result<PaycheckParseResult> =
        messageParseService.parsePaycheckFromText(text)

    suspend fun parseDieselFromText(text: String): Result<DieselParseResult> =
        messageParseService.parseDieselFromText(text)
}
