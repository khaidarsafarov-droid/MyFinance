package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt

/**
 * Goal progress bar for the home-screen widget.
 *
 * The percent label sits in [headroomPx] **above** the track so it stays
 * readable even when the fill is a sliver (e.g. 9%). Bitmap height is
 * exactly [barHeightPx] + [headroomPx] so Glance shows it 1:1.
 */
object WidgetTruckProgressBitmap {

    fun create(
        context: Context,
        progressPercent: Float,
        goalSet: Boolean,
        widthPx: Int,
        barHeightPx: Int,
        colors: WidgetCabinColors,
        headroomPx: Int = defaultHeadroomPx(barHeightPx),
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(48)
        val trackHeight = barHeightPx.coerceAtLeast(10)
        val headroom = headroomPx.coerceAtLeast(trackHeight)
        val safeHeight = trackHeight + headroom
        val bitmap = createBitmap(safeWidth, safeHeight)
        val canvas = Canvas(bitmap)
        val corner = trackHeight / 2f
        val barTop = headroom.toFloat()
        val barBottom = barTop + trackHeight
        val bounds = RectF(0f, barTop, safeWidth.toFloat(), barBottom)

        canvas.drawRoundRect(
            bounds,
            corner,
            corner,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.ringTrack
                alpha = 200
            },
        )

        val progress = if (goalSet) progressPercent.coerceIn(0f, 100f) else 0f
        val fillWidth = safeWidth * (progress / 100f)
        if (fillWidth > 1f) {
            canvas.drawRoundRect(
                RectF(0f, barTop, fillWidth, barBottom),
                corner,
                corner,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, barTop, fillWidth.coerceAtLeast(1f), barTop,
                        colors.progressStart, colors.progressEnd, Shader.TileMode.CLAMP,
                    )
                },
            )
        }
        if (goalSet) {
            drawPercentLabel(
                canvas = canvas,
                label = WidgetStatsFormatter.formatRingPercent(progress),
                safeWidth = safeWidth,
                headroom = headroom,
                color = colors.text,
            )
        }
        return bitmap
    }

    fun defaultHeadroomPx(barHeightPx: Int): Int =
        (barHeightPx.coerceAtLeast(10) * 1.55f).roundToInt()

    /**
     * Large percent in the headroom — sized from headroom height, not the
     * thin track, so "9.2%" stays readable on a short fill.
     */
    internal fun percentTextSizePx(headroomPx: Int): Float =
        (headroomPx.coerceAtLeast(16) * 0.78f).coerceAtLeast(18f)

    private fun drawPercentLabel(
        canvas: Canvas,
        label: String,
        safeWidth: Int,
        headroom: Int,
        color: Int,
    ) {
        val pad = (headroom * 0.10f).coerceAtLeast(4f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            textSize = percentTextSizePx(headroom)
            setShadowLayer(2.5f, 0f, 1f, 0x66000000)
        }
        val measured = textPaint.measureText(label)
        val maxWidth = (safeWidth - pad * 2).coerceAtLeast(8f)
        if (measured > maxWidth) {
            textPaint.textSize = textPaint.textSize * maxWidth / measured
        }
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val baseline = ((headroom - textHeight) / 2f - fm.ascent)
            .coerceIn(-fm.ascent + 1f, headroom - fm.descent - 1f)
        canvas.drawText(label, pad, baseline, textPaint)
    }
}
