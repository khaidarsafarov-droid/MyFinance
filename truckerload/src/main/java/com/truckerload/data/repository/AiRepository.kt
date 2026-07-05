package com.truckerload.data.repository

import com.truckerload.data.remote.AiService
import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult
import kotlinx.coroutines.flow.Flow

class AiRepository(private val aiService: AiService) {

    fun chatStream(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String? = null
    ): Flow<String> =
        aiService.chatStream(history, userMessage, appContext)

    suspend fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String? = null
    ): Result<String> =
        aiService.chat(history, userMessage, appContext)

    suspend fun extractTextFromImage(imageBytes: ByteArray, mimeType: String = "image/jpeg"): Result<String> =
        aiService.extractTextFromImage(imageBytes, mimeType)

    suspend fun parseLoadFromMessage(rawMessage: String): Result<Load> =
        aiService.parseLoadFromMessage(rawMessage)

    suspend fun parseLoadsFromMessage(rawMessage: String): Result<List<Load>> =
        aiService.parseLoadsFromMessage(rawMessage)

    suspend fun parsePaycheckFromText(text: String): Result<PaycheckParseResult> =
        aiService.parsePaycheckFromText(text)

    suspend fun parseDieselFromText(text: String): Result<DieselParseResult> =
        aiService.parseDieselFromText(text)

    suspend fun healthCheck(): Result<String> =
        aiService.healthCheck()

    suspend fun generateRealTimeLogisticsInsight(
        userName: String,
        rpm: Double,
        profit: Double,
        fuelCost: Double,
        miles: Double,
        topStates: List<String>,
        anomalies: String
    ): Result<AiService.RealTimeLogisticsInsight> =
        aiService.generateRealTimeLogisticsInsight(
            userName = userName,
            rpm = rpm,
            profit = profit,
            fuelCost = fuelCost,
            miles = miles,
            topStates = topStates,
            anomalies = anomalies
        )
}
