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
 *
 * The truck is one filled silhouette (van ∪ tires) so wheels cannot float
 * under a detached chassis line.
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
        // Never invent extra headroom — Glance Image height must match this bitmap.
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
                hubColor = 0xFFFFFFFF.toInt(),
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
        hubColor: Int,
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
            val hubFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = hubColor
                style = Paint.Style.FILL
            }
            val tireStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = geom.stroke
            }

            // One silhouette: van body ∪ tire disks — no gap possible.
            drawPath(buildTruckSilhouette(geom), body)
            drawPath(buildTruckOutline(geom), stroke)
            drawHubs(this, geom, hubFill, tireStroke)
            drawCabGlass(this, geom)
        }
    }

    /**
     * Wheel outer bottoms land on local y = h (= barTop after translate).
     * Tire disks are unioned into the body so the chassis never floats.
     */
    private data class TruckGeom(val w: Float, val h: Float) {
        val stroke: Float = (h * 0.045f).coerceAtLeast(1.4f)
        val wheelRadius: Float = h * 0.20f
        /**
         * Outer tire stroke sits fully on/above local y = h (= barTop).
         * Using a full stroke clearance avoids AA bleeding into the track.
         */
        val wheelCy: Float = h - wheelRadius - stroke
        /** Chassis cuts through the hubs so van and tires read as one piece. */
        val deck: Float = wheelCy + wheelRadius * 0.28f
        val roof: Float = h * 0.06f
        val hubRadius: Float = wheelRadius * 0.36f
        /** Side outlines stop above the tires so no deck stroke cuts through wheels. */
        val sideStop: Float = wheelCy - wheelRadius * 0.85f
        val wheelXs: FloatArray = floatArrayOf(w * 0.17f, w * 0.32f, w * 0.82f)
    }

    private fun buildTruckSilhouette(geom: TruckGeom): Path {
        val p = Path().apply { fillType = Path.FillType.WINDING }
        p.addPath(buildTrailerPath(geom))
        p.addPath(buildHitchPath(geom))
        p.addPath(buildCabPath(geom))
        // Union tire disks into the same fill — wheels are part of the truck.
        geom.wheelXs.forEach { cx ->
            p.addCircle(cx, geom.wheelCy, geom.wheelRadius, Path.Direction.CW)
        }
        // Continuous chassis skirt through the axles (kills any body↔tire gap).
        val skirtTop = geom.deck - geom.wheelRadius * 0.55f
        val skirtBottom = (geom.wheelCy + geom.wheelRadius * 0.15f).coerceAtMost(geom.h - geom.stroke)
        p.addRoundRect(
            RectF(geom.w * 0.08f, skirtTop, geom.w * 0.92f, skirtBottom),
            geom.wheelRadius * 0.35f,
            geom.wheelRadius * 0.35f,
            Path.Direction.CW,
        )
        return p
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
        p.lineTo(w * 0.97f, deck * 0.88f)
        p.lineTo(w * 0.97f, deck)
        p.close()
        return p
    }

    /**
     * Van outline only above the tires. A deck-level stroke through the wheel
     * disks is what made the body look sliced off from the wheels.
     */
    private fun buildTruckOutline(geom: TruckGeom): Path {
        val p = Path()
        val left = geom.w * 0.02f
        val right = geom.w * 0.52f
        val top = geom.roof
        val stop = geom.sideStop
        val rx = geom.w * 0.05f
        p.moveTo(left, stop)
        p.lineTo(left, top + rx)
        p.quadTo(left, top, left + rx, top)
        p.lineTo(right - rx, top)
        p.quadTo(right, top, right, top + rx)
        p.lineTo(right, stop)

        // Hitch: top edge only (no verticals down into the tire zone).
        val hitchTop = (geom.deck - geom.h * 0.14f).coerceAtMost(stop)
        p.moveTo(geom.w * 0.50f, hitchTop)
        p.lineTo(geom.w * 0.58f, hitchTop)

        val w = geom.w
        val h = geom.h
        val roof = geom.roof
        val sleeperLeft = w * 0.575f
        p.moveTo(sleeperLeft, stop)
        p.lineTo(sleeperLeft, roof + h * 0.05f)
        p.quadTo(sleeperLeft, roof, sleeperLeft + w * 0.035f, roof)
        p.lineTo(w * 0.70f, roof)
        p.lineTo(w * 0.78f, h * 0.26f)
        p.lineTo(w * 0.90f, h * 0.26f)
        p.lineTo(w * 0.97f, stop)
        return p
    }

    private fun drawHubs(canvas: Canvas, geom: TruckGeom, hubFill: Paint, tireStroke: Paint) {
        geom.wheelXs.forEach { cx ->
            // Hub only in the lower part of the tire so the upper half stays
            // body-colored — reads as a wheel tucked under the chassis.
            canvas.withSave {
                clipRect(
                    cx - geom.hubRadius - geom.stroke,
                    geom.wheelCy - geom.hubRadius * 0.15f,
                    cx + geom.hubRadius + geom.stroke,
                    geom.h + geom.stroke,
                )
                drawCircle(cx, geom.wheelCy, geom.hubRadius, hubFill)
                drawCircle(cx, geom.wheelCy, geom.hubRadius, tireStroke)
            }
            canvas.drawCircle(cx, geom.wheelCy, geom.wheelRadius, tireStroke)
        }
    }

    private fun drawCabGlass(canvas: Canvas, geom: TruckGeom) {
        val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(geom.w * 0.80f, geom.h * 0.30f, geom.w * 0.90f, geom.deck - geom.h * 0.08f),
            geom.w * 0.02f,
            geom.w * 0.02f,
            glass,
        )
    }
}
