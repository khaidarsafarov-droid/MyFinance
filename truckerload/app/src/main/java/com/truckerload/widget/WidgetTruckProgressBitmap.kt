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
        val truckWidth = (truckHeight * 1.92f).roundToInt()
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
                trackHeight = trackHeight.toFloat(),
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
     * Conventional US semi (trailer + sleeper + hood), facing right.
     * [leadingX] is the bumper; wheels rest on the progress track.
     */
    private fun drawMockupTruck(
        canvas: Canvas,
        leadingX: Float,
        barBottom: Float,
        truckWidth: Float,
        truckHeight: Float,
        trackHeight: Float,
        progressStart: Int,
        progressEnd: Int,
        outlineColor: Int,
    ) {
        val left = leadingX - truckWidth
        val top = barBottom - truckHeight
        canvas.withSave {
            translate(left, top)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    truckWidth,
                    0f,
                    progressStart,
                    progressEnd,
                    Shader.TileMode.CLAMP,
                )
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = outlineColor
                style = Paint.Style.STROKE
                strokeWidth = (truckHeight * 0.032f).coerceAtLeast(1.15f)
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            val trailer = buildTrailerPath(truckWidth, truckHeight)
            val tractor = buildTractorPath(truckWidth, truckHeight)
            drawPath(trailer, fillPaint)
            drawPath(tractor, fillPaint)
            drawPath(trailer, strokePaint)
            drawPath(tractor, strokePaint)
            drawCabWindow(
                canvas = this,
                truckWidth = truckWidth,
                truckHeight = truckHeight,
            )
            drawHeadlight(this, truckWidth, truckHeight)
            drawTruckWheels(
                canvas = this,
                truckWidth = truckWidth,
                truckHeight = truckHeight,
                trackHeight = trackHeight,
                outlineColor = outlineColor,
            )
        }
    }

    /** Dry van sitting on the fifth-wheel, rounded box. */
    private fun buildTrailerPath(width: Float, height: Float): Path {
        val p = Path()
        val box = RectF(width * 0.03f, height * 0.14f, width * 0.54f, height * 0.56f)
        val rx = width * 0.045f
        p.addRoundRect(box, rx, rx, Path.Direction.CW)
        return p
    }

    /** Sleeper + cab + sloped hood (conventional tractor). */
    private fun buildTractorPath(width: Float, height: Float): Path {
        val p = Path()
        val w = width
        val h = height
        val deck = h * 0.56f
        val sleeperLeft = w * 0.52f
        val roof = h * 0.12f
        p.moveTo(sleeperLeft, deck)
        p.lineTo(sleeperLeft, roof + h * 0.05f)
        p.quadTo(sleeperLeft, roof, sleeperLeft + w * 0.04f, roof)
        p.lineTo(w * 0.70f, roof)
        p.lineTo(w * 0.73f, h * 0.20f)
        p.lineTo(w * 0.82f, h * 0.20f)
        p.lineTo(w * 0.93f, h * 0.34f)
        p.quadTo(w * 0.98f, h * 0.38f, w * 0.98f, deck)
        p.close()
        return p
    }

    private fun drawCabWindow(
        canvas: Canvas,
        truckWidth: Float,
        truckHeight: Float,
    ) {
        val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xF2FFFFFF.toInt()
            style = Paint.Style.FILL
        }
        val path = Path()
        path.moveTo(truckWidth * 0.735f, truckHeight * 0.23f)
        path.lineTo(truckWidth * 0.82f, truckHeight * 0.23f)
        path.lineTo(truckWidth * 0.90f, truckHeight * 0.35f)
        path.lineTo(truckWidth * 0.735f, truckHeight * 0.35f)
        path.close()
        canvas.drawPath(path, glass)
    }

    private fun drawHeadlight(canvas: Canvas, truckWidth: Float, truckHeight: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF4C2.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            RectF(
                truckWidth * 0.925f,
                truckHeight * 0.40f,
                truckWidth * 0.975f,
                truckHeight * 0.49f,
            ),
            paint,
        )
    }

    private fun drawTruckWheels(
        canvas: Canvas,
        truckWidth: Float,
        truckHeight: Float,
        trackHeight: Float,
        outlineColor: Int,
    ) {
        val radius = truckHeight * 0.11f
        val restY = truckHeight - trackHeight
        val cy = restY - radius * 0.18f
        val wheelXs = floatArrayOf(truckWidth * 0.16f, truckWidth * 0.32f, truckWidth * 0.80f)
        val tire = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = outlineColor }
        val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        wheelXs.forEach { cx ->
            canvas.drawCircle(cx, cy, radius, tire)
            canvas.drawCircle(cx, cy, radius * 0.42f, hub)
        }
    }
}
