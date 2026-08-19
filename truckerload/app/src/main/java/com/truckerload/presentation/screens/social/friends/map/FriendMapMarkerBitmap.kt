package com.truckerload.presentation.screens.social.friends.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import com.truckerload.domain.friends.FriendMapLabels
import java.net.HttpURLConnection
import java.net.URL

internal object FriendMapMarkerBitmap {
    const val RING_ME = 0xFF16A34A.toInt()
    const val RING_FRIEND = 0xFFEA580C.toInt()
    const val RING_FRIEND_SELECTED = 0xFF2563EB.toInt()
    const val DEST_FILL = 0xFF4B5563.toInt()

    fun loadPhoto(path: String?, maxPx: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val decoded = when {
                path.startsWith("http://") || path.startsWith("https://") -> decodeHttp(path)
                path.startsWith("file://") || path.startsWith("content://") -> {
                    val filePath = Uri.parse(path).path ?: return@runCatching null
                    BitmapFactory.decodeFile(filePath)
                }
                else -> BitmapFactory.decodeFile(path)
            } ?: return@runCatching null
            scaleSquare(decoded, maxPx)
        }.getOrNull()
    }

    fun createPerson(density: Float, label: String, ringColor: Int, photo: Bitmap?): Bitmap {
        val dp = density.coerceAtLeast(1f)
        val avatar = (48f * dp).toInt().coerceAtLeast(40)
        val ring = 3.5f * dp
        val pointerH = 10f * dp
        val hPad = 8f * dp
        val vPad = 6f * dp
        val gap = 4f * dp
        val textSize = 12f * dp
        val caption = FriendMapLabels.ellipsize(label.ifBlank { "?" })
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111827.toInt()
            this.textSize = textSize
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val textW = textPaint.measureText(caption)
        val textH = textPaint.fontMetrics.let { it.descent - it.ascent }
        val pillH = textH + 8f * dp
        val pillW = textW + 16f * dp
        val avatarOuter = avatar + ring * 2f
        val width = maxOf(avatarOuter + hPad * 2f, pillW + hPad * 2f).toInt().coerceAtLeast(1)
        val height = (vPad + pillH + gap + avatarOuter + pointerH).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = width / 2f

        val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000 }
        val pillRect = RectF(
            cx - pillW / 2f,
            vPad,
            cx + pillW / 2f,
            vPad + pillH,
        )
        canvas.drawRoundRect(pillRect.offsetCopy(0f, 1.5f * dp), pillH / 2f, pillH / 2f, shadow)
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pill)
        val textY = pillRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(caption, cx, textY, textPaint)

        val avatarCy = vPad + pillH + gap + avatarOuter / 2f
        val radius = avatar / 2f
        canvas.drawCircle(cx, avatarCy + 1.5f * dp, radius + ring, shadow)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringColor }
        canvas.drawCircle(cx, avatarCy, radius + ring, ringPaint)
        if (photo != null) {
            val circled = circleCrop(photo, avatar)
            canvas.drawBitmap(circled, cx - radius, avatarCy - radius, null)
            if (circled != photo) circled.recycle()
        } else {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringColor }
            canvas.drawCircle(cx, avatarCy, radius, fill)
            val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                this.textSize = 16f * dp
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val initials = FriendMapLabels.initials(caption)
            val iy = avatarCy - (initialPaint.ascent() + initialPaint.descent()) / 2f
            canvas.drawText(initials, cx, iy, initialPaint)
        }

        val tipY = avatarCy + radius + ring + pointerH
        val pointer = Path().apply {
            moveTo(cx - 7f * dp, avatarCy + radius + ring - 1f)
            lineTo(cx + 7f * dp, avatarCy + radius + ring - 1f)
            lineTo(cx, tipY)
            close()
        }
        canvas.drawPath(pointer, ringPaint)
        return bmp
    }

    fun createDestination(density: Float, label: String): Bitmap {
        val dp = density.coerceAtLeast(1f)
        val caption = FriendMapLabels.ellipsize(label.ifBlank { "DEL" }, 14)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111827.toInt()
            textSize = 11f * dp
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val textW = textPaint.measureText(caption)
        val textH = textPaint.fontMetrics.let { it.descent - it.ascent }
        val pillH = textH + 6f * dp
        val pillW = textW + 14f * dp
        val pin = 18f * dp
        val width = (pillW + 12f * dp).toInt().coerceAtLeast(1)
        val height = (6f * dp + pillH + 4f * dp + pin + 8f * dp).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = width / 2f
        val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val pillRect = RectF(cx - pillW / 2f, 4f * dp, cx + pillW / 2f, 4f * dp + pillH)
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pill)
        val textY = pillRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(caption, cx, textY, textPaint)
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DEST_FILL }
        val pinCy = 4f * dp + pillH + 4f * dp + pin / 2f
        canvas.drawCircle(cx, pinCy, pin / 2f, pinPaint)
        val tip = Path().apply {
            moveTo(cx - 6f * dp, pinCy + 2f * dp)
            lineTo(cx + 6f * dp, pinCy + 2f * dp)
            lineTo(cx, pinCy + pin / 2f + 8f * dp)
            close()
        }
        canvas.drawPath(tip, pinPaint)
        return bmp
    }

    private fun decodeHttp(url: String): Bitmap? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2_500
            readTimeout = 2_500
            instanceFollowRedirects = true
        }
        return try {
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            conn.disconnect()
        }
    }

    private fun scaleSquare(src: Bitmap, size: Int): Bitmap {
        if (src.width == size && src.height == size) return src
        val scale = size.toFloat() / minOf(src.width, src.height).coerceAtLeast(1)
        val matrix = Matrix().apply { setScale(scale, scale) }
        val scaled = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        val x = ((scaled.width - size) / 2).coerceAtLeast(0)
        val y = ((scaled.height - size) / 2).coerceAtLeast(0)
        val w = size.coerceAtMost(scaled.width)
        val h = size.coerceAtMost(scaled.height)
        return Bitmap.createBitmap(scaled, x, y, w, h)
    }

    private fun circleCrop(src: Bitmap, size: Int): Bitmap {
        val squared = scaleSquare(src, size)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)
        return out
    }

    private fun RectF.offsetCopy(dx: Float, dy: Float): RectF =
        RectF(left + dx, top + dy, right + dx, bottom + dy)
}
