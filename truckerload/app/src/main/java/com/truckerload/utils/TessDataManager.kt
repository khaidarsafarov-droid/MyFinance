package com.truckerload.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads Tesseract traineddata (eng + rus) on first OCR use.
 * Models: tessdata_fast (~4 MB each) from the official Tesseract project.
 */
object TessDataManager {

    private const val TESSDATA_BASE_URL = "https://github.com/tesseract-ocr/tessdata_fast/raw/main"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun tessDataDir(context: Context): File =
        File(context.filesDir, "tessdata").apply { mkdirs() }

    suspend fun ensureTessData(context: Context): Boolean = withContext(Dispatchers.IO) {
        listOf("eng", "rus").all { lang ->
            val file = File(tessDataDir(context), "$lang.traineddata")
            if (file.exists() && file.length() > 50_000) return@all true
            downloadFile("$TESSDATA_BASE_URL/$lang.traineddata", file)
        }
    }

    private fun downloadFile(url: String, dest: File): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                dest.outputStream().use { out -> body.byteStream().copyTo(out) }
                dest.exists() && dest.length() > 50_000
            }
        } catch (_: Exception) {
            false
        }
    }
}
