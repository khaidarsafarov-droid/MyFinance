package com.truckerload.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoManager(private val context: Context) {

    private val photosDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "photos")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun savePhoto(bitmap: Bitmap, locationData: LocationData, timestamp: Long = System.currentTimeMillis()): File {
        val fileName = "photo_${timestampFormat.format(Date(timestamp))}.jpg"
        val file = File(photosDir, fileName)
        val watermarked = addWatermark(bitmap, locationData, timestamp)
        FileOutputStream(file).use { out ->
            watermarked.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        if (watermarked !== bitmap) {
            watermarked.recycle()
        }
        return file
    }

    fun addWatermark(bitmap: Bitmap, locationData: LocationData, timestamp: Long): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val scale = result.width / 1080f
        val padding = 40f * scale
        val lineSpacing = 14f * scale
        val textSize = 40f * scale

        val unknown = context.getString(com.truckerload.R.string.watermark_unknown)
        val appName = context.getString(com.truckerload.R.string.watermark_app_name)
        val dateLine = formatDateTime(timestamp)
        val lines = listOf(
            appName,
            dateLine,
            locationData.cityStateLine.ifBlank { unknown },
            locationData.zipCode.ifBlank { unknown },
            if (locationData.latitude == 0.0 && locationData.longitude == 0.0) {
                unknown
            } else {
                locationData.coordinatesLine
            },
        )

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.BLACK)
        }

        val lineHeight = textPaint.fontSpacing
        var currentY = padding + lineHeight

        lines.forEach { line ->
            val textWidth = textPaint.measureText(line)
            val x = result.width - padding - textWidth
            canvas.drawText(line, x, currentY, textPaint)
            currentY += lineHeight + lineSpacing
        }

        return result
    }

    companion object {
        private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        fun formatDateTime(timestamp: Long): String =
            dateTimeFormat.format(Date(timestamp))
    }
}
