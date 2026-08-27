package com.truckerload.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.truckerload.R
import java.io.ByteArrayOutputStream

/** Encodes [R.drawable.app_logo] as a square JPEG for Telegram `setMyProfilePhoto`. */
object TelegramBotLogoJpeg {

    const val SIZE_PX = 640
    const val JPEG_QUALITY = 90
    private val plate = Color.parseColor("#5B54E6")

    fun encode(context: Context): ByteArray? {
        val src = loadLogoBitmap(context) ?: return null
        try {
            return encodeBitmap(src)
        } finally {
            if (!src.isRecycled) src.recycle()
        }
    }

    fun encodeBitmap(src: Bitmap): ByteArray? {
        val dest = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(dest)
            canvas.drawColor(plate)
            val scaled = Bitmap.createScaledBitmap(src, SIZE_PX, SIZE_PX, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            if (scaled !== src && !scaled.isRecycled) scaled.recycle()
            val out = ByteArrayOutputStream()
            if (!dest.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) return null
            val bytes = out.toByteArray()
            return bytes.takeIf { isJpeg(it) }
        } finally {
            if (!dest.isRecycled) dest.recycle()
        }
    }

    fun loadLogoBitmap(context: Context): Bitmap? {
        val fromPng = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        }.getOrNull()
        if (fromPng != null) return fromPng
        val fallback = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_image)
        }.getOrNull()
        if (fallback != null) return fallback
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: return null
        val bmp = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(plate)
        drawable.setBounds(0, 0, SIZE_PX, SIZE_PX)
        drawable.draw(canvas)
        return bmp
    }

    fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
}
