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
import java.util.concurrent.atomic.AtomicReference

class PhotoManager(private val context: Context) {

    private val photosDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "photos")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun savePhoto(
        bitmap: Bitmap,
        locationData: LocationData,
        timestamp: Long = System.currentTimeMillis(),
        tripId: String? = null,
        loadDate: String? = null,
        watermarkTitle: String? = null,
    ): File {
        val fileName = if (!tripId.isNullOrBlank()) {
            AttachmentNaming.buildFileName(tripId, loadDate.orEmpty(), timestamp, "jpg")
        } else {
            "photo_${formatTimestampFile(timestamp)}_${timestamp}.jpg"
        }
        val file = File(photosDir, fileName)
        val scaled = scaleDownIfNeeded(bitmap, MAX_EDGE_PX)
        val watermarked = addWatermark(
            bitmap = scaled,
            locationData = locationData,
            timestamp = timestamp,
            title = watermarkTitle?.takeIf { it.isNotBlank() } ?: tripId,
        )
        if (scaled !== bitmap) scaled.recycle()
        FileOutputStream(file).use { out ->
            watermarked.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        if (watermarked !== bitmap) {
            watermarked.recycle()
        }
        return file
    }

    fun addWatermark(
        bitmap: Bitmap,
        locationData: LocationData,
        timestamp: Long,
        title: String? = null,
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val scale = result.width / 1080f
        val padding = 40f * scale
        val lineSpacing = 14f * scale
        val textSize = 40f * scale

        val unknown = context.getString(com.truckerload.R.string.watermark_unknown)
        val header = title?.takeIf { it.isNotBlank() }
            ?: context.getString(com.truckerload.R.string.watermark_app_name)
        val dateLine = formatDateTime(timestamp)
        val lines = listOf(
            header,
            dateLine,
            locationData.cityStateLine.ifBlank { unknown },
            locationData.zipCode.ifBlank { unknown },
            if (!locationData.hasCoordinates) {
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
        const val JPEG_QUALITY = 90
        const val MAX_EDGE_PX = 2048

        private val timestampPattern = "yyyyMMdd_HHmmss"
        private val dateTimePattern = "dd.MM.yyyy HH:mm"
        private val timestampFormatRef = AtomicReference(SimpleDateFormat(timestampPattern, Locale.US))
        private val dateTimeFormatRef = AtomicReference(SimpleDateFormat(dateTimePattern, Locale.getDefault()))

        private fun formatTimestampFile(timestamp: Long): String =
            synchronized(timestampFormatRef) {
                timestampFormatRef.get().format(Date(timestamp))
            }

        fun formatDateTime(timestamp: Long): String =
            synchronized(dateTimeFormatRef) {
                dateTimeFormatRef.get().format(Date(timestamp))
            }

        fun scaleDownIfNeeded(bitmap: Bitmap, maxEdge: Int = MAX_EDGE_PX): Bitmap {
            val maxDim = maxOf(bitmap.width, bitmap.height)
            if (maxDim <= maxEdge) return bitmap
            val scale = maxEdge.toFloat() / maxDim
            val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(bitmap, w, h, true)
        }

        /** Pure title resolution for tests (no Android Context). */
        fun resolveWatermarkTitle(watermarkTitle: String?, tripId: String?, fallback: String): String =
            watermarkTitle?.takeIf { it.isNotBlank() }
                ?: tripId?.takeIf { it.isNotBlank() }
                ?: fallback
    }
}
