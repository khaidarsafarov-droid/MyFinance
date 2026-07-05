package com.truckerload.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TesseractOCRService(private val context: Context) {

    private val mutex = Mutex()
    private var tessBase: TessBaseAPI? = null

    suspend fun recognizeFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        if (!TessDataManager.ensureTessData(context)) return@withContext ""
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@withContext ""
        try {
            recognizeBitmap(bitmap)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    suspend fun recognizeBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        if (!TessDataManager.ensureTessData(context)) return@withContext ""
        mutex.withLock {
            val tess = getOrInitTess() ?: return@withContext ""
            tess.setImage(bitmap)
            tess.getUTF8Text()?.trim().orEmpty()
        }
    }

    private fun getOrInitTess(): TessBaseAPI? {
        tessBase?.let { return it }
        return try {
            val dataPath = context.filesDir.absolutePath
            TessBaseAPI().also { tess ->
                val ok = tess.init(dataPath, "rus+eng")
                if (!ok) {
                    tess.recycle()
                    return null
                }
                tessBase = tess
            }
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        tessBase?.recycle()
        tessBase = null
    }
}
