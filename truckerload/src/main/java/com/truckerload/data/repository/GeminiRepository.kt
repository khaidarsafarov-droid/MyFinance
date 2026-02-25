package com.truckerload.data.repository

import com.truckerload.data.remote.GeminiService
import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult

class GeminiRepository(private val geminiService: GeminiService) {

    suspend fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String? = null
    ): Result<String> =
        geminiService.chat(history, userMessage, appContext)

    suspend fun extractTextFromImage(imageBytes: ByteArray, mimeType: String = "image/jpeg"): Result<String> =
        geminiService.extractTextFromImage(imageBytes, mimeType)

    suspend fun parseLoadFromMessage(rawMessage: String): Result<Load> =
        geminiService.parseLoadFromMessage(rawMessage)

    suspend fun parseLoadsFromMessage(rawMessage: String): Result<List<Load>> =
        geminiService.parseLoadsFromMessage(rawMessage)

    suspend fun parsePaycheckFromText(text: String): Result<PaycheckParseResult> =
        geminiService.parsePaycheckFromText(text)

    suspend fun parseDieselFromText(text: String): Result<DieselParseResult> =
        geminiService.parseDieselFromText(text)
}
