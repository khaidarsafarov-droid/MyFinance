package com.truckerload.utils.ocr

import android.content.Context
import android.net.Uri
import com.truckerload.utils.OCRService
import com.truckerload.utils.OcrResult

/**
 * Hybrid OCR facade: ML Kit Latin + Tesseract rus+eng.
 * See [OCRService] for implementation details.
 */
class HybridOCRService(context: Context) {

    private val appContext = context.applicationContext
    private val delegate = OCRService(appContext)

    suspend fun recognizeFromUri(uri: Uri): OcrResult =
        delegate.recognizeFromUri(appContext, uri)

    fun detectedLanguage(text: String): String = LanguageDetector.detect(text)

    fun close() = delegate.close()
}
