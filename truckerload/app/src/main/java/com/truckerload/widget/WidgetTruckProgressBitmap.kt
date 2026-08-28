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
     * Conventional US semi facing right. Deck + fender skirts sit on the tires
     * so the body and wheels read as one truck; tire bottoms on the progress baseline.
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
        val geom = truckGeom(truckWidth, truckHeight, trackHeight)
        canvas.withSave {
            translate(left, top)

            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x3A000000.toInt()
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
                    truckHeight * 0.08f,
                    truckWidth,
                    truckHeight * 0.08f,
                    progressStart,
                    progressEnd,
                    Shader.TileMode.CLAMP,
                )
            }
            val bodyShade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f,
                    truckHeight * 0.10f,
                    0f,
                    geom.deck,
                    0x33FFFFFF.toInt(),
                    0x22000000.toInt(),
                    Shader.TileMode.CLAMP,
                )
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = outlineColor
                style = Paint.Style.STROKE
                strokeWidth = (truckHeight * 0.026f).coerceAtLeast(1.1f)
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            val chassisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = outlineColor
                style = Paint.Style.FILL
                alpha = 220
            }

            val trailer = buildTrailerPath(geom)
            val tractor = buildTractorPath(geom)
            val hitch = RectF(
                truckWidth * 0.485f,
                geom.deck - truckHeight * 0.08f,
                truckWidth * 0.575f,
                geom.deck,
            )

            // Wheels first; dark underbody + van drawn on top so tires tuck under
            // the truck instead of floating as separate circles.
            drawTruckWheels(this, geom, outlineColor)
            drawChassis(this, geom, chassisPaint)
            drawPath(trailer, bodyFill)
            drawPath(trailer, bodyShade)
            drawRoundRect(hitch, truckWidth * 0.015f, truckWidth * 0.015f, bodyFill)
            drawPath(tractor, bodyFill)
            drawPath(tractor, bodyShade)
            drawFenderSkirts(this, geom, bodyFill)

            drawTrailerRibs(this, geom, outlineColor)
            drawPath(trailer, strokePaint)
            drawRoundRect(hitch, truckWidth * 0.015f, truckWidth * 0.015f, strokePaint)
            drawPath(tractor, strokePaint)
            // Cover the deck stroke over the tires so the outline doesn't sever them.
            drawFenderSkirts(this, geom, bodyFill)
            drawChassis(this, geom, chassisPaint)
            // Re-draw only the lower tire caps so hubs stay visible under the skirt.
            drawTruckWheelBottoms(this, geom, outlineColor)

            drawCabWindow(this, geom, outlineColor)
            drawGrilleAndBumper(this, geom, outlineColor)
            drawHeadlight(this, geom)
        }
    }

    /**
     * Body sits on the progress-bar top; tires rest on the bar bottom; a dark
     * chassis bridges the two so the marker reads as one truck.
     */
    private data class TruckGeom(
        val w: Float,
        val h: Float,
        val wheelRadius: Float,
        val wheelCy: Float,
        val deck: Float,
        val roof: Float,
        val wheelXs: FloatArray,
    )

    private fun truckGeom(w: Float, h: Float, trackHeight: Float): TruckGeom {
        val wheelRadius = (trackHeight * 0.42f).coerceIn(h * 0.12f, h * 0.16f)
        val wheelCy = h - wheelRadius
        // Progress-bar top in local coords (y=h is the bar bottom / tire baseline).
        val barTop = (h - trackHeight).coerceAtLeast(h * 0.35f)
        // Colored van dips into the bar; dark skirt covers the upper tires.
        val deck = (barTop + trackHeight * 0.18f).coerceAtMost(wheelCy - wheelRadius * 0.20f)
        return TruckGeom(
            w = w,
            h = h,
            wheelRadius = wheelRadius,
            wheelCy = wheelCy,
            deck = deck,
            roof = h * 0.05f,
            wheelXs = floatArrayOf(w * 0.16f, w * 0.30f, w * 0.84f),
        )
    }

    private fun buildTrailerPath(geom: TruckGeom): Path {
        val p = Path()
        val box = RectF(geom.w * 0.02f, geom.roof + geom.h * 0.02f, geom.w * 0.50f, geom.deck)
        p.addRoundRect(box, geom.w * 0.038f, geom.w * 0.038f, Path.Direction.CW)
        return p
    }

    private fun buildTractorPath(geom: TruckGeom): Path {
        val p = Path()
        val w = geom.w
        val h = geom.h
        val deck = geom.deck
        val sleeperLeft = w * 0.555f
        val roof = geom.roof
        p.moveTo(sleeperLeft, deck)
        p.lineTo(sleeperLeft, roof + h * 0.04f)
        p.quadTo(sleeperLeft, roof, sleeperLeft + w * 0.035f, roof)
        p.lineTo(w * 0.695f, roof)
        p.lineTo(w * 0.735f, h * 0.18f)
        p.lineTo(w * 0.825f, h * 0.18f)
        p.lineTo(w * 0.935f, deck * 0.72f)
        p.quadTo(w * 0.99f, deck * 0.82f, w * 0.99f, deck)
        p.close()
        return p
    }

    /** Solid dark underbody that covers the upper half of each tire. */
    private fun drawChassis(canvas: Canvas, geom: TruckGeom, chassisPaint: Paint) {
        val skirtBottom = geom.wheelCy + geom.wheelRadius * 0.20f
        val skirt = RectF(
            geom.w * 0.04f,
            geom.deck - geom.h * 0.015f,
            geom.w * 0.97f,
            skirtBottom,
        )
        canvas.drawRoundRect(skirt, geom.w * 0.025f, geom.w * 0.025f, chassisPaint)
        val padHalf = geom.wheelRadius * 1.05f
        geom.wheelXs.forEach { cx ->
            canvas.drawRoundRect(
                RectF(
                    cx - padHalf,
                    geom.deck - geom.h * 0.01f,
                    cx + padHalf,
                    geom.wheelCy + geom.wheelRadius * 0.15f,
                ),
                geom.wheelRadius * 0.45f,
                geom.wheelRadius * 0.45f,
                chassisPaint,
            )
        }
    }

    /** Body-colored lip blending the van into the underbody over each axle. */
    private fun drawFenderSkirts(canvas: Canvas, geom: TruckGeom, bodyFill: Paint) {
        val drop = geom.wheelRadius * 0.35f
        val halfW = geom.wheelRadius * 1.2f
        geom.wheelXs.forEach { cx ->
            canvas.drawOval(
                RectF(cx - halfW, geom.deck - drop * 0.3f, cx + halfW, geom.deck + drop),
                bodyFill,
            )
        }
    }

    private fun drawTrailerRibs(canvas: Canvas, geom: TruckGeom, outlineColor: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            alpha = 55
            strokeWidth = (geom.h * 0.012f).coerceAtLeast(0.8f)
            strokeCap = Paint.Cap.ROUND
        }
        val y1 = geom.h * 0.18f
        val y2 = geom.deck - geom.h * 0.08f
        floatArrayOf(0.14f, 0.26f, 0.38f).forEach { xFrac ->
            canvas.drawLine(geom.w * xFrac, y1, geom.w * xFrac, y2, paint)
        }
        val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF.toInt()
            strokeWidth = (geom.h * 0.018f).coerceAtLeast(1f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(geom.w * 0.06f, geom.h * 0.14f, geom.w * 0.46f, geom.h * 0.14f, gloss)
    }

    private fun drawCabWindow(canvas: Canvas, geom: TruckGeom, outlineColor: Int) {
        val glassTop = geom.h * 0.14f
        val glassBottom = geom.h * 0.34f
        val glass = Path()
        glass.moveTo(geom.w * 0.74f, glassTop)
        glass.lineTo(geom.w * 0.825f, glassTop)
        glass.lineTo(geom.w * 0.905f, glassBottom)
        glass.lineTo(geom.w * 0.74f, glassBottom)
        glass.close()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                geom.w * 0.74f,
                glassTop,
                geom.w * 0.90f,
                glassBottom,
                0xE8FFFFFF.toInt(),
                0xA8C8D8FF.toInt(),
                Shader.TileMode.CLAMP,
            )
        }
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = (geom.h * 0.016f).coerceAtLeast(0.9f)
        }
        canvas.drawPath(glass, fill)
        canvas.drawPath(glass, frame)
        val sleeper = RectF(geom.w * 0.58f, geom.h * 0.16f, geom.w * 0.68f, geom.h * 0.32f)
        canvas.drawRoundRect(sleeper, geom.w * 0.012f, geom.w * 0.012f, fill)
        canvas.drawRoundRect(sleeper, geom.w * 0.012f, geom.w * 0.012f, frame)
    }

    private fun drawGrilleAndBumper(canvas: Canvas, geom: TruckGeom, outlineColor: Int) {
        val grille = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            alpha = 90
            strokeWidth = (geom.h * 0.012f).coerceAtLeast(0.8f)
            strokeCap = Paint.Cap.ROUND
        }
        val x1 = geom.w * 0.86f
        val x2 = geom.w * 0.94f
        val mid = (geom.h * 0.34f + geom.deck) * 0.5f
        floatArrayOf(mid - geom.h * 0.05f, mid, mid + geom.h * 0.05f).forEach { y ->
            canvas.drawLine(x1, y, x2, y, grille)
        }
        val bumper = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            alpha = 140
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(
                geom.w * 0.955f,
                geom.deck - geom.h * 0.07f,
                geom.w * 0.995f,
                geom.deck,
            ),
            geom.w * 0.008f,
            geom.w * 0.008f,
            bumper,
        )
    }

    private fun drawHeadlight(canvas: Canvas, geom: TruckGeom) {
        val cx = geom.w * 0.958f
        val cy = geom.deck - geom.h * 0.16f
        val rx = geom.w * 0.018f
        val ry = geom.h * 0.032f
        val lens = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF1B8.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), lens)
    }


    /** Lower half of each tire, redrawn after the skirt so hubs stay visible. */
    private fun drawTruckWheelBottoms(canvas: Canvas, geom: TruckGeom, outlineColor: Int) {
        val radius = geom.wheelRadius
        val cy = geom.wheelCy
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
        geom.wheelXs.forEach { cx ->
            // Clip to the lower portion of the tire (below the skirt overlap).
            canvas.save()
            canvas.clipRect(cx - radius - 1f, cy - radius * 0.15f, cx + radius + 1f, cy + radius + 1f)
            canvas.drawCircle(cx, cy, radius, tire)
            canvas.drawCircle(cx, cy, radius * 0.55f, rim)
            canvas.drawCircle(cx, cy, radius * 0.22f, hub)
            canvas.restore()
        }
    }

    private fun drawTruckWheels(canvas: Canvas, geom: TruckGeom, outlineColor: Int) {
        val radius = geom.wheelRadius
        val cy = geom.wheelCy
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
        geom.wheelXs.forEach { cx ->
            canvas.drawCircle(cx, cy, radius, tire)
            canvas.drawCircle(cx, cy, radius * 0.55f, rim)
            canvas.drawCircle(cx, cy, radius * 0.22f, hub)
        }
    }
}
