package com.truckerload.data.repository

import com.truckerload.domain.advisor.DeterministicAdvisorService
import com.truckerload.domain.advisor.LogisticsInsight
import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult
import com.truckerload.domain.parser.AmazonRelayParseResult
import com.truckerload.domain.parser.MessageParseService
import kotlinx.coroutines.flow.Flow

/**
 * Facade for parsing and local advisor logic — no external AI providers.
 */
class AiRepository(
    private val messageParseService: MessageParseService = MessageParseService(),
    private val advisorService: DeterministicAdvisorService = DeterministicAdvisorService()
) {

    fun chatStream(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String? = null
    ): Flow<String> = advisorService.chatStream(history, userMessage, appContext)

    suspend fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String? = null
    ): Result<String> = advisorService.chat(history, userMessage, appContext)

    suspend fun extractTextFromImage(imageBytes: ByteArray, mimeType: String): Result<String> =
        Result.failure(UnsupportedOperationException(OCR_IMAGE_DISABLED_CODE))

    companion object {
        /** Callers should map this to [com.truckerload.R.string.ocr_image_disabled]. */
        const val OCR_IMAGE_DISABLED_CODE = "OCR_IMAGE_DISABLED"
    }

    suspend fun parseLoadFromMessage(rawMessage: String): Result<Load> =
        messageParseService.parseLoadFromMessage(rawMessage)

    suspend fun parseAmazonRelayFromMessage(rawMessage: String): Result<AmazonRelayParseResult> =
        messageParseService.parseAmazonRelayFromMessage(rawMessage)

    suspend fun parseLoadsFromMessage(rawMessage: String): Result<List<Load>> =
        messageParseService.parseLoadsFromMessage(rawMessage)

    suspend fun parsePaycheckFromText(text: String): Result<PaycheckParseResult> =
        messageParseService.parsePaycheckFromText(text)

    suspend fun parseDieselFromText(text: String): Result<DieselParseResult> =
        messageParseService.parseDieselFromText(text)

    suspend fun healthCheck(): Result<String> = advisorService.healthCheck()

    suspend fun generateRealTimeLogisticsInsight(
        userName: String,
        rpm: Double,
        profit: Double,
        fuelCost: Double,
        miles: Double,
        topStates: List<String>,
        anomalies: String
    ): Result<LogisticsInsight> = advisorService.generateInsight(
        rpm = rpm,
        profit = profit,
        fuelCost = fuelCost,
        miles = miles,
        topStates = topStates,
        anomalies = anomalies
    )
}
