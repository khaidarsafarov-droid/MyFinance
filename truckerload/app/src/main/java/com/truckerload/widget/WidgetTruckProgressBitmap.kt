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

/** Horizontal goal progress bar with a polished semi-truck marker (lavender → teal). */
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
        val truckHeight = (trackHeight * 2.95f).roundToInt().coerceIn(30, 76)
        // Slightly shorter nose-to-tail so the marker reads cleaner on narrow widgets.
        val truckWidth = (truckHeight * 1.72f).roundToInt()
        val headroom = (truckHeight - trackHeight).coerceAtLeast(10)
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
                truckWidth * 0.58f,
                safeWidth - truckWidth * 0.06f,
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

    /** Subtle motion dashes trailing the trailer (kept quiet for a product look). */
    private fun drawSpeedLines(
        canvas: Canvas,
        truckLeadingX: Float,
        truckWidth: Float,
        barCenterY: Float,
        trackHeight: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE8E4FF.toInt()
            strokeCap = Paint.Cap.ROUND
        }
        val cargoLeft = truckLeadingX - truckWidth * 0.94f
        val specs = listOf(
            Triple(-0.12f, 0.22f, 110),
            Triple(0.00f, 0.30f, 140),
            Triple(0.12f, 0.18f, 100),
        )
        specs.forEach { (yOff, lengthFrac, alpha) ->
            paint.alpha = alpha
            paint.strokeWidth = (trackHeight * 0.055f).coerceAtLeast(0.9f)
            val y = barCenterY + yOff * trackHeight
            val length = truckWidth * lengthFrac
            val startX = (cargoLeft - truckWidth * 0.26f).coerceAtLeast(0f)
            canvas.drawLine(startX, y, startX + length, y, paint)
        }
    }

    /**
     * Professional conventional US semi facing right: dry-van trailer, fifth-wheel
     * hitch, sleeper, cab, hood — wheels resting on the progress track.
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

            // Soft contact shadow on the progress baseline (under the tires).
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x3A000000
                style = Paint.Style.FILL
            }
            drawOval(
                RectF(truckWidth * 0.10f, truckHeight * 0.90f, truckWidth * 0.92f, truckHeight * 0.995f),
                shadow,
            )

            val bodyFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f,
                    truckHeight * 0.10f,
                    truckWidth,
                    truckHeight * 0.10f,
                    progressStart,
                    progressEnd,
                    Shader.TileMode.CLAMP,
                )
            }
            val bodyShade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f,
                    truckHeight * 0.14f,
                    0f,
                    truckHeight * 0.58f,
                    0x33FFFFFF,
                    0x22000000,
                    Shader.TileMode.CLAMP,
                )
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = outlineColor
                style = Paint.Style.STROKE
                strokeWidth = (truckHeight * 0.028f).coerceAtLeast(1.1f)
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

            val trailer = buildTrailerPath(truckWidth, truckHeight)
            val tractor = buildTractorPath(truckWidth, truckHeight)
            val hitch = RectF(
                truckWidth * 0.485f,
                truckHeight * 0.50f,
                truckWidth * 0.575f,
                truckHeight * 0.58f,
            )

            drawPath(trailer, bodyFill)
            drawPath(trailer, bodyShade)
            drawRoundRect(hitch, truckWidth * 0.015f, truckWidth * 0.015f, bodyFill)
            drawPath(tractor, bodyFill)
            drawPath(tractor, bodyShade)

            drawTrailerRibs(this, truckWidth, truckHeight, outlineColor)
            drawPath(trailer, strokePaint)
            drawRoundRect(hitch, truckWidth * 0.015f, truckWidth * 0.015f, strokePaint)
            drawPath(tractor, strokePaint)

            drawCabWindow(this, truckWidth, truckHeight, outlineColor)
            drawGrilleAndBumper(this, truckWidth, truckHeight, outlineColor)
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

    /** Dry van with rounded corners. */
    private fun buildTrailerPath(width: Float, height: Float): Path {
        val p = Path()
        val box = RectF(width * 0.02f, height * 0.10f, width * 0.50f, height * 0.58f)
        val rx = width * 0.038f
        p.addRoundRect(box, rx, rx, Path.Direction.CW)
        return p
    }

    /** Sleeper + windshield rake + sloped hood. */
    private fun buildTractorPath(width: Float, height: Float): Path {
        val p = Path()
        val w = width
        val h = height
        val deck = h * 0.58f
        val sleeperLeft = w * 0.555f
        val roof = h * 0.09f
        p.moveTo(sleeperLeft, deck)
        p.lineTo(sleeperLeft, roof + h * 0.04f)
        p.quadTo(sleeperLeft, roof, sleeperLeft + w * 0.035f, roof)
        p.lineTo(w * 0.695f, roof)
        // Windshield rake
        p.lineTo(w * 0.735f, h * 0.18f)
        p.lineTo(w * 0.825f, h * 0.18f)
        // Hood slope to bumper
        p.lineTo(w * 0.935f, h * 0.32f)
        p.quadTo(w * 0.99f, h * 0.36f, w * 0.99f, deck)
        p.close()
        return p
    }

    private fun drawTrailerRibs(
        canvas: Canvas,
        truckWidth: Float,
        truckHeight: Float,
        outlineColor: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            alpha = 55
            strokeWidth = (truckHeight * 0.012f).coerceAtLeast(0.8f)
            strokeCap = Paint.Cap.ROUND
        }
        val y1 = truckHeight * 0.22f
        val y2 = truckHeight * 0.46f
        floatArrayOf(0.14f, 0.26f, 0.38f).forEach { xFrac ->
            val x = truckWidth * xFrac
            canvas.drawLine(x, y1, x, y2, paint)
        }
        // Top highlight edge on the van
        val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF
            strokeWidth = (truckHeight * 0.018f).coerceAtLeast(1f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            truckWidth * 0.06f,
            truckHeight * 0.16f,
            truckWidth * 0.46f,
            truckHeight * 0.16f,
            gloss,
        )
    }

    private fun drawCabWindow(
        canvas: Canvas,
        truckWidth: Float,
        truckHeight: Float,
        outlineColor: Int,
    ) {
        val glass = Path()
        glass.moveTo(truckWidth * 0.74f, truckHeight * 0.215f)
        glass.lineTo(truckWidth * 0.825f, truckHeight * 0.215f)
        glass.lineTo(truckWidth * 0.905f, truckHeight * 0.345f)
        glass.lineTo(truckWidth * 0.74f, truckHeight * 0.345f)
        glass.close()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                truckWidth * 0.74f,
                truckHeight * 0.215f,
                truckWidth * 0.90f,
                truckHeight * 0.345f,
                0xE8FFFFFF.toInt(),
                0xA8C8D8FF.toInt(),
                Shader.TileMode.CLAMP,
            )
        }
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = (truckHeight * 0.016f).coerceAtLeast(0.9f)
        }
        canvas.drawPath(glass, fill)
        canvas.drawPath(glass, frame)
        // Side sleeper window
        val sleeper = RectF(
            truckWidth * 0.58f,
            truckHeight * 0.20f,
            truckWidth * 0.68f,
            truckHeight * 0.34f,
        )
        canvas.drawRoundRect(sleeper, truckWidth * 0.012f, truckWidth * 0.012f, fill)
        canvas.drawRoundRect(sleeper, truckWidth * 0.012f, truckWidth * 0.012f, frame)
    }

    private fun drawGrilleAndBumper(
        canvas: Canvas,
        truckWidth: Float,
        truckHeight: Float,
        outlineColor: Int,
    ) {
        val grille = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            alpha = 90
            strokeWidth = (truckHeight * 0.012f).coerceAtLeast(0.8f)
            strokeCap = Paint.Cap.ROUND
        }
        val x1 = truckWidth * 0.86f
        val x2 = truckWidth * 0.94f
        floatArrayOf(0.40f, 0.445f, 0.49f).forEach { yFrac ->
            canvas.drawLine(x1, truckHeight * yFrac, x2, truckHeight * yFrac, grille)
        }
        val bumper = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            alpha = 140
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(
                truckWidth * 0.955f,
                truckHeight * 0.50f,
                truckWidth * 0.995f,
                truckHeight * 0.56f,
            ),
            truckWidth * 0.008f,
            truckWidth * 0.008f,
            bumper,
        )
    }

    private fun drawHeadlight(canvas: Canvas, truckWidth: Float, truckHeight: Float) {
        val cx = truckWidth * 0.958f
        val cy = truckHeight * 0.42f
        val rx = truckWidth * 0.018f
        val ry = truckHeight * 0.032f
        val lens = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF1B8.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), lens)
    }

    private fun drawTruckWheels(
        canvas: Canvas,
        truckWidth: Float,
        truckHeight: Float,
        trackHeight: Float,
        outlineColor: Int,
    ) {
        // Larger tires; bottom of each tire sits exactly on the progress-bar baseline
        // (local y = truckHeight maps to barBottom in create()).
        val radius = truckHeight * 0.138f
        val cy = truckHeight - radius
        // Trailer tandem + drive axle (tighter spacing for the shorter body).
        val wheelXs = floatArrayOf(
            truckWidth * 0.16f,
            truckWidth * 0.30f,
            truckWidth * 0.84f,
        )
        val tire = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            style = Paint.Style.FILL
        }
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE8E4F8.toInt()
            style = Paint.Style.FILL
        }
        val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            alpha = 180
            style = Paint.Style.FILL
        }
        wheelXs.forEach { cx ->
            canvas.drawCircle(cx, cy, radius, tire)
            canvas.drawCircle(cx, cy, radius * 0.55f, rim)
            canvas.drawCircle(cx, cy, radius * 0.22f, hub)
        }
    }
}
