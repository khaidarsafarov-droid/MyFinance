package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import kotlin.math.roundToInt

/** Horizontal goal progress bar with mockup-style truck marker (lavender → teal). */
object WidgetTruckProgressBitmap {

    fun create(
        context: Context,
        progressPercent: Float,
        goalSet: Boolean,
        widthPx: Int,
        barHeightPx: Int,
        colors: WidgetCabinColors,
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(48)
        val trackHeight = barHeightPx.coerceAtLeast(10)
        val truckHeight = (trackHeight * 2.85f).roundToInt().coerceIn(28, 72)
        val truckWidth = (truckHeight * 1.55f).roundToInt()
        val headroom = (truckHeight - trackHeight).coerceAtLeast(8)
        val safeHeight = trackHeight + headroom
        val bitmap = createBitmap(safeWidth, safeHeight)
        val canvas = Canvas(bitmap)
        val corner = trackHeight / 2f
        val barTop = headroom.toFloat()
        val barBottom = barTop + trackHeight
        val bounds = RectF(0f, barTop, safeWidth.toFloat(), barBottom)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.ringTrack
            alpha = 200
        }
        canvas.drawRoundRect(bounds, corner, corner, trackPaint)

        val progressStart = colors.progressStart
        val progressEnd = colors.progressEnd
        val progress = if (goalSet) progressPercent.coerceIn(0f, 100f) else 0f
        val fillWidth = safeWidth * (progress / 100f)
        if (fillWidth > 1f) {
            val fillRect = RectF(0f, barTop, fillWidth, barBottom)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    barTop,
                    fillWidth.coerceAtLeast(1f),
                    barTop,
                    progressStart,
                    progressEnd,
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRoundRect(fillRect, corner, corner, fillPaint)

            if (goalSet && progress > 0f) {
                val label = WidgetStatsFormatter.formatRingPercent(progress)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.progressLabel
                    textSize = trackHeight * 0.58f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val textX = trackHeight * 0.35f
                canvas.drawText(
                    label,
                    textX.coerceAtMost((fillWidth - trackHeight * 0.5f).coerceAtLeast(trackHeight * 0.2f)),
                    barTop + trackHeight * 0.71f,
                    textPaint,
                )
            }
        }

        if (goalSet && progress > 0f) {
            val truckLeadingX = fillWidth.coerceIn(
                truckWidth * 0.55f,
                safeWidth - truckWidth * 0.08f,
            )
            drawSpeedLines(
                canvas = canvas,
                truckLeadingX = truckLeadingX,
                truckWidth = truckWidth.toFloat(),
                barCenterY = (barTop + barBottom) / 2f,
                trackHeight = trackHeight.toFloat(),
            )
            drawMockupTruck(
                canvas = canvas,
                leadingX = truckLeadingX,
                barBottom = barBottom,
                truckWidth = truckWidth.toFloat(),
                truckHeight = truckHeight.toFloat(),
                progressStart = progressStart,
                progressEnd = progressEnd,
                outlineColor = colors.progressLabel,
            )
        }

        return bitmap
    }

    /** Three white motion dashes trailing behind the cargo box (mockup). */
    private fun drawSpeedLines(
        canvas: Canvas,
        truckLeadingX: Float,
        truckWidth: Float,
        barCenterY: Float,
        trackHeight: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE8E4FF.toInt()
            alpha = 220
            strokeWidth = (trackHeight * 0.08f).coerceAtLeast(1.1f)
            strokeCap = Paint.Cap.ROUND
        }
        val cargoLeft = truckLeadingX - truckWidth * 0.92f
        val lineLengths = floatArrayOf(0.28f, 0.38f, 0.22f)
        val yOffsets = floatArrayOf(-0.14f, 0f, 0.14f)
        lineLengths.indices.forEach { index ->
            val y = barCenterY + yOffsets[index] * trackHeight
            val length = truckWidth * lineLengths[index]
            val startX = (cargoLeft - truckWidth * 0.34f).coerceAtLeast(0f)
            canvas.drawLine(startX, y, startX + length, y, paint)
        }
    }

    /**
     * Side-profile semi-truck: lavender→teal body, dark outline, three wheels.
     * [leadingX] is the front (cab) x; wheels sit on [barBottom].
     */
    private fun drawMockupTruck(
        canvas: Canvas,
        leadingX: Float,
        barBottom: Float,
        truckWidth: Float,
        truckHeight: Float,
        progressStart: Int,
        progressEnd: Int,
        outlineColor: Int,
    ) {
        val left = leadingX - truckWidth
        val top = barBottom - truckHeight
        val body = buildTruckBodyPath(truckWidth, truckHeight)
        canvas.withSave {
            translate(left, top)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    truckWidth,
                    truckHeight,
                    progressStart,
                    progressEnd,
                    Shader.TileMode.CLAMP,
                )
            }
            drawPath(body, fillPaint)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = outlineColor
                style = Paint.Style.STROKE
                strokeWidth = (truckHeight * 0.045f).coerceAtLeast(1.4f)
                strokeJoin = Paint.Join.ROUND
            }
            drawPath(body, strokePaint)
            drawCabWindow(
                canvas = this,
                truckWidth = truckWidth,
                truckHeight = truckHeight,
                outlineColor = outlineColor,
            )
            drawTruckWheels(
                canvas = this,
                truckWidth = truckWidth,
                truckHeight = truckHeight,
                outlineColor = outlineColor,
            )
        }
    }

    private fun buildTruckBodyPath(width: Float, height: Float): Path {
        val p = Path()
        val w = width
        val h = height
        val cargoLeft = w * 0.04f
        val cargoRight = w * 0.58f
        val cargoTop = h * 0.18f
        val cargoBottom = h * 0.72f
        val cabRight = w * 0.98f
        val cabTop = h * 0.22f
        val hoodTop = h * 0.30f

        p.moveTo(cargoLeft, cargoTop)
        p.lineTo(cargoRight, cargoTop)
        p.lineTo(cargoRight, cargoBottom)
        p.lineTo(w * 0.62f, cargoBottom)
        p.lineTo(w * 0.62f, hoodTop)
        p.lineTo(w * 0.78f, cabTop)
        p.lineTo(cabRight, cabTop)
        p.lineTo(cabRight, cargoBottom)
        p.lineTo(cargoLeft, cargoBottom)
        p.close()
        return p
    }

    private fun drawCabWindow(
        canvas: Canvas,
        truckWidth: Float,
        truckHeight: Float,
        outlineColor: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE6FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = (truckHeight * 0.028f).coerceAtLeast(1f)
            strokeCap = Paint.Cap.ROUND
        }
        val x1 = truckWidth * 0.66f
        val y1 = truckHeight * 0.24f
        val x2 = truckWidth * 0.94f
        val y2 = truckHeight * 0.42f
        canvas.drawLine(x1, y1, x2, y2, paint)
        canvas.drawLine(x1, y2, x2, y2, paint)
    }

    private fun drawTruckWheels(
        canvas: Canvas,
        truckWidth: Float,
        truckHeight: Float,
        outlineColor: Int,
    ) {
        val radius = truckHeight * 0.095f
        val cy = truckHeight * 0.76f
        val wheelXs = floatArrayOf(truckWidth * 0.22f, truckWidth * 0.42f, truckWidth * 0.82f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = (truckHeight * 0.035f).coerceAtLeast(1.1f)
        }
        wheelXs.forEach { cx ->
            canvas.drawCircle(cx, cy, radius, fillPaint)
            canvas.drawCircle(cx, cy, radius, strokePaint)
        }
    }
}
