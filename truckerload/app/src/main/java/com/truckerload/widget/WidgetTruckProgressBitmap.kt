package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import kotlin.math.roundToInt

/**
 * Goal progress bar with a flat semi that rides on the **top** of the track.
 *
 * Bitmap height is exactly [barHeightPx] + [headroomPx] so Glance shows it 1:1
 * (no vertical squash that tears body from wheels).
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

        val progressStart = colors.progressStart
        val progressEnd = colors.progressEnd
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
                        progressStart, progressEnd, Shader.TileMode.CLAMP,
                    )
                },
            )
            if (goalSet && progress > 0f) {
                val label = WidgetStatsFormatter.formatRingPercent(progress)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.progressLabel
                    textSize = trackHeight * 0.58f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(
                    label,
                    (trackHeight * 0.35f).coerceAtMost(
                        (fillWidth - trackHeight * 0.5f).coerceAtLeast(trackHeight * 0.2f),
                    ),
                    barTop + trackHeight * 0.71f,
                    textPaint,
                )
            }
        }

        if (goalSet && progress > 0f) {
            val truckHeight = (headroom * 0.92f).roundToInt().coerceIn(18, headroom)
            val truckWidth = (truckHeight * 1.55f).roundToInt()
            val truckLeadingX = fillWidth.coerceIn(
                truckWidth * 0.55f,
                safeWidth - truckWidth * 0.04f,
            )
            drawSpeedLines(canvas, truckLeadingX, truckWidth.toFloat(), barTop, truckHeight.toFloat())
            drawFlatTruck(
                canvas = canvas,
                leadingX = truckLeadingX,
                barTop = barTop,
                truckWidth = truckWidth.toFloat(),
                truckHeight = truckHeight.toFloat(),
                bodyColor = truckBodyColor(colors),
                strokeColor = truckStrokeColor(colors),
                wheelColor = 0xFFFFFFFF.toInt(),
            )
        }
        return bitmap
    }

    fun defaultHeadroomPx(barHeightPx: Int): Int =
        (barHeightPx.coerceAtLeast(10) * 1.55f).roundToInt()

    private fun truckBodyColor(colors: WidgetCabinColors): Int =
        if (Color.luminance(colors.bg) < 0.45f) 0xFFE6E1F2.toInt() else colors.brand

    private fun truckStrokeColor(colors: WidgetCabinColors): Int =
        if (Color.luminance(colors.bg) < 0.45f) 0xFF2A2540.toInt() else 0xFF1E1A2E.toInt()

    private fun drawSpeedLines(
        canvas: Canvas,
        truckLeadingX: Float,
        truckWidth: Float,
        barTop: Float,
        truckHeight: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB8B0D0.toInt()
            strokeCap = Paint.Cap.ROUND
        }
        val cargoLeft = truckLeadingX - truckWidth * 0.96f
        val bodyMidY = barTop - truckHeight * 0.50f
        listOf(
            Triple(-0.10f, 0.14f, 140),
            Triple(0.00f, 0.20f, 170),
            Triple(0.10f, 0.12f, 130),
        ).forEach { (yOff, lengthFrac, alpha) ->
            paint.alpha = alpha
            paint.strokeWidth = (truckHeight * 0.04f).coerceAtLeast(1.2f)
            val y = bodyMidY + yOff * truckHeight
            val startX = (cargoLeft - truckWidth * 0.16f).coerceAtLeast(0f)
            canvas.drawLine(startX, y, startX + truckWidth * lengthFrac, y, paint)
        }
    }

    private fun drawFlatTruck(
        canvas: Canvas,
        leadingX: Float,
        barTop: Float,
        truckWidth: Float,
        truckHeight: Float,
        bodyColor: Int,
        strokeColor: Int,
        wheelColor: Int,
    ) {
        val geom = TruckGeom(truckWidth, truckHeight)
        canvas.withSave {
            translate(leadingX - truckWidth, barTop - truckHeight)

            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bodyColor
                style = Paint.Style.FILL
            }
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = geom.stroke
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            val wheelFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = wheelColor
                style = Paint.Style.FILL
            }
            val wheelStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = geom.stroke
            }

            drawWheels(this, geom, wheelFill, wheelStroke)
            val trailer = buildTrailerPath(geom)
            val hitch = buildHitchPath(geom)
            val cab = buildCabPath(geom)
            drawPath(trailer, body)
            drawPath(hitch, body)
            drawPath(cab, body)
            drawFenderCaps(this, geom, body)
            drawPath(trailer, stroke)
            drawPath(hitch, stroke)
            drawPath(cab, stroke)
            drawFenderCaps(this, geom, body)
            drawWheelBottoms(this, geom, wheelFill, wheelStroke)
        }
    }

    /**
     * Wheel outer bottoms land on local y = h (= barTop).
     * Deck overlaps upper tires so van + wheels read as one truck.
     */
    private data class TruckGeom(val w: Float, val h: Float) {
        val stroke: Float = (h * 0.045f).coerceAtLeast(1.4f)
        val wheelRadius: Float = h * 0.175f
        val wheelCy: Float = h - wheelRadius - stroke * 0.5f
        val deck: Float = wheelCy - wheelRadius * 0.55f
        val roof: Float = h * 0.05f
        val wheelXs: FloatArray = floatArrayOf(w * 0.17f, w * 0.31f, w * 0.82f)
    }

    private fun buildTrailerPath(geom: TruckGeom): Path {
        val p = Path()
        val left = geom.w * 0.02f
        val right = geom.w * 0.52f
        val top = geom.roof
        val deck = geom.deck
        val rx = geom.w * 0.05f
        p.moveTo(left, deck)
        p.lineTo(left, top + rx)
        p.quadTo(left, top, left + rx, top)
        p.lineTo(right - rx, top)
        p.quadTo(right, top, right, top + rx)
        p.lineTo(right, deck)
        p.close()
        return p
    }

    private fun buildHitchPath(geom: TruckGeom): Path {
        val p = Path()
        p.addRoundRect(
            RectF(geom.w * 0.50f, geom.deck - geom.h * 0.14f, geom.w * 0.58f, geom.deck),
            geom.w * 0.012f,
            geom.w * 0.012f,
            Path.Direction.CW,
        )
        return p
    }

    private fun buildCabPath(geom: TruckGeom): Path {
        val p = Path()
        val w = geom.w
        val h = geom.h
        val deck = geom.deck
        val roof = geom.roof
        val sleeperLeft = w * 0.575f
        p.moveTo(sleeperLeft, deck)
        p.lineTo(sleeperLeft, roof + h * 0.05f)
        p.quadTo(sleeperLeft, roof, sleeperLeft + w * 0.035f, roof)
        p.lineTo(w * 0.70f, roof)
        p.lineTo(w * 0.78f, h * 0.26f)
        p.lineTo(w * 0.90f, h * 0.26f)
        p.lineTo(w * 0.97f, deck * 0.82f)
        p.lineTo(w * 0.97f, deck)
        p.close()
        return p
    }

    private fun drawFenderCaps(canvas: Canvas, geom: TruckGeom, body: Paint) {
        val drop = geom.wheelRadius * 0.55f
        val halfW = geom.wheelRadius * 1.25f
        geom.wheelXs.forEach { cx ->
            canvas.drawOval(
                RectF(cx - halfW, geom.deck - drop * 0.15f, cx + halfW, geom.deck + drop),
                body,
            )
        }
    }

    private fun drawWheels(canvas: Canvas, geom: TruckGeom, fill: Paint, stroke: Paint) {
        geom.wheelXs.forEach { cx ->
            canvas.drawCircle(cx, geom.wheelCy, geom.wheelRadius, fill)
            canvas.drawCircle(cx, geom.wheelCy, geom.wheelRadius, stroke)
        }
    }

    private fun drawWheelBottoms(canvas: Canvas, geom: TruckGeom, fill: Paint, stroke: Paint) {
        geom.wheelXs.forEach { cx ->
            canvas.withSave {
                clipRect(
                    cx - geom.wheelRadius - geom.stroke,
                    geom.wheelCy - geom.wheelRadius * 0.05f,
                    cx + geom.wheelRadius + geom.stroke,
                    geom.h + geom.stroke,
                )
                drawCircle(cx, geom.wheelCy, geom.wheelRadius, fill)
                drawCircle(cx, geom.wheelCy, geom.wheelRadius, stroke)
            }
        }
    }
}
