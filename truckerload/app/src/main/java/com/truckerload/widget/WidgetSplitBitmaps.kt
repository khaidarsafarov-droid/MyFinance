package com.truckerload.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/** Header gradient and goal bar. Glance has no brush, so the strip is a bitmap. */
internal object WidgetSplitBitmaps {
    fun gradient(
        widthPx: Int,
        heightPx: Int,
        startColor: Int,
        endColor: Int,
        topRadiusPx: Float,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                0f,
                startColor,
                endColor,
                Shader.TileMode.CLAMP,
            )
        }
        val radius = topRadiusPx.coerceAtLeast(0f)
        val radii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        val path = Path().apply {
            addRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                radii,
                Path.Direction.CW,
            )
        }
        canvas.drawPath(path, paint)
        return bitmap
    }

    fun bar(
        widthPx: Int,
        heightPx: Int,
        fraction: Float,
        fill: Int,
        track: Int,
        radiusPx: Float = heightPx / 2f,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = radiusPx.coerceIn(0f, height / 2f)
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        paint.color = track
        canvas.drawRoundRect(bounds, radius, radius, paint)
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped > 0f) {
            val span = (width * clamped).coerceAtLeast(height.toFloat()).coerceAtMost(width.toFloat())
            paint.color = fill
            canvas.drawRoundRect(RectF(0f, 0f, span, height.toFloat()), radius, radius, paint)
        }
        return bitmap
    }
}
