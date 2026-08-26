package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.truckerload.R
import kotlin.math.roundToInt

/** Horizontal goal progress bar with truck marker — mockup-style Glance bitmap. */
object WidgetTruckProgressBitmap {

    fun create(
        context: Context,
        progressPercent: Float,
        goalSet: Boolean,
        widthPx: Int,
        heightPx: Int,
        colors: WidgetCabinColors,
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(48)
        val safeHeight = heightPx.coerceAtLeast(10)
        val bitmap = createBitmap(safeWidth, safeHeight)
        val canvas = Canvas(bitmap)
        val corner = safeHeight / 2f
        val bounds = RectF(0f, 0f, safeWidth.toFloat(), safeHeight.toFloat())

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.ringTrack }
        canvas.drawRoundRect(bounds, corner, corner, trackPaint)

        val progress = if (goalSet) progressPercent.coerceIn(0f, 100f) else 0f
        val fillWidth = safeWidth * (progress / 100f)
        if (fillWidth > 1f) {
            val fillRect = RectF(0f, 0f, fillWidth, safeHeight.toFloat())
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    fillWidth.coerceAtLeast(1f),
                    0f,
                    colors.ring,
                    colors.progressEnd,
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRoundRect(fillRect, corner, corner, fillPaint)

            if (goalSet && progress >= 4f) {
                val label = WidgetStatsFormatter.formatRingPercent(progress)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.onFilled
                    textSize = safeHeight * 0.52f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val pad = safeHeight * 0.28f
                canvas.drawText(label, pad, safeHeight * 0.72f, textPaint)
            }
        }

        if (goalSet && progress > 0f) {
            val truckSize = (safeHeight * 1.35f).roundToInt().coerceIn(14, safeHeight + 8)
            val truckX = fillWidth.coerceIn(
                truckSize / 2f,
                safeWidth - truckSize / 2f,
            )
            drawTruck(context, canvas, truckX, safeHeight / 2f, truckSize, colors.onFilled)
        }

        return bitmap
    }

    private fun drawTruck(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        sizePx: Int,
        tint: Int,
    ) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.widget_ic_truck) ?: return
        drawable.setTint(tint)
        val half = sizePx / 2
        val left = (centerX - half).roundToInt()
        val top = (centerY - half).roundToInt()
        drawable.bounds = Rect(left, top, left + sizePx, top + sizePx)
        drawable.draw(canvas)
    }
}
