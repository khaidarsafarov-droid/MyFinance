package com.truckerload.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import com.truckerload.R
import java.io.ByteArrayOutputStream

/** Encodes [R.drawable.app_logo] as a square JPEG for Telegram `setMyProfilePhoto`. */
object TelegramBotLogoJpeg {

    const val SIZE_PX = 640
    const val JPEG_QUALITY = 90
    private val navy = Color.parseColor("#143882")

    fun encode(context: Context): ByteArray? {
        val src = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo) ?: return null
        val dest = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(dest)
            canvas.drawColor(navy)
            val scaled = Bitmap.createScaledBitmap(src, SIZE_PX, SIZE_PX, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            if (scaled !== src) scaled.recycle()
            val out = ByteArrayOutputStream()
            if (!dest.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) return null
            val bytes = out.toByteArray()
            return bytes.takeIf { isJpeg(it) }
        } finally {
            src.recycle()
            dest.recycle()
        }
    }

    fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
}
