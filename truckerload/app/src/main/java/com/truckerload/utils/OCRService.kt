package com.truckerload.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.truckerload.utils.ocr.LanguageDetector
import com.truckerload.utils.ocr.OcrLine
import com.truckerload.utils.ocr.OcrReadingOrder
import kotlinx.coroutines.tasks.await

data class OcrResult(
    val text: String,
    val usedRussianEngine: Boolean,
    val detectedLanguage: String = LanguageDetector.detect(text),
)

class OCRService(context: Context) {

    private val appContext = context.applicationContext
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val tesseract = TesseractOCRService(appContext)

    suspend fun recognizeFromUri(context: Context, uri: Uri): OcrResult {
        val mlKitText = recognizeLatinFromUri(uri)
        val needsTesseract = mlKitText.isBlank() ||
            mlKitText.length < 8 ||
            containsCyrillic(mlKitText)
        val tesseractText = if (needsTesseract) {
            tesseract.recognizeFromUri(uri)
        } else {
            ""
        }
        return mergeResults(mlKitText, tesseractText)
    }

    suspend fun recognizeScanResult(context: Context, result: GmsDocumentScanningResult): OcrResult {
        val pages = result.pages ?: return OcrResult("", false)
        if (pages.isEmpty()) return OcrResult("", false)
        val pageResults = pages.mapNotNull { page ->
            page.imageUri?.let { uri -> recognizeFromUri(context, uri) }
        }
        if (pageResults.isEmpty()) return OcrResult("", false)
        val combined = pageResults.joinToString(separator = "\n\n—\n\n") { it.text }
        val usedRussian = pageResults.any { it.usedRussianEngine }
        return OcrResult(combined.trim(), usedRussian, LanguageDetector.detect(combined))
    }

    private suspend fun recognizeLatinFromUri(uri: Uri): String {
        return try {
            val image = InputImage.fromFilePath(appContext, uri)
            val recognized = latinRecognizer.process(image).await()
            val ordered = OcrReadingOrder.join(
                recognized.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        OcrLine(
                            top = line.boundingBox?.top ?: 0,
                            left = line.boundingBox?.left ?: 0,
                            text = line.text,
                        )
                    }
                },
            )
            ordered.ifBlank { recognized.text.trim() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: MlKitException) {
            logFailure("OCRService", "latin recognition failed", e)
            ""
        } catch (e: java.io.IOException) {
            logFailure("OCRService", "latin recognition failed", e)
            ""
        }
    }

    private fun mergeResults(mlKitText: String, tesseractText: String): OcrResult {
        val tesseractBetter = when {
            mlKitText.isBlank() -> tesseractText
            tesseractText.isBlank() -> mlKitText
            containsCyrillic(tesseractText) && !containsCyrillic(mlKitText) -> tesseractText
            tesseractText.length > mlKitText.length * 1.2 -> tesseractText
            else -> mlKitText
        }
        val usedRussian = tesseractText.isNotBlank() &&
            (tesseractBetter == tesseractText || containsCyrillic(tesseractBetter))
        return OcrResult(tesseractBetter.trim(), usedRussian, LanguageDetector.detect(tesseractBetter))
    }

    fun close() {
        latinRecognizer.close()
        tesseract.close()
    }

    companion object {
        fun containsCyrillic(text: String): Boolean =
            text.any { it in '\u0400'..'\u04FF' }
    }
}
