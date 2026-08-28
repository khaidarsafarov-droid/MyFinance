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
 * Goal progress bar with a flat semi-truck marker that rides on the **top**
 * edge of the track (wheel bottoms touch [barTop], like the TruckoRig reference).
 */
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
        // Compact flat silhouette that sits above the bar (wheel bottoms on barTop).
        // Height ≈ 1.45× track so Glance headroomDp (~1.4–1.6× bar) keeps proportions.
        val truckHeight = (trackHeight * 1.45f).roundToInt().coerceIn(22, 48)
        val truckWidth = (truckHeight * 1.58f).roundToInt()
        val headroom = truckHeight
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
                safeWidth - truckWidth * 0.04f,
            )
            drawSpeedLines(
                canvas = canvas,
                truckLeadingX = truckLeadingX,
                truckWidth = truckWidth.toFloat(),
                barTop = barTop,
                truckHeight = truckHeight.toFloat(),
            )
            drawFlatTruck(
                canvas = canvas,
                leadingX = truckLeadingX,
                barTop = barTop,
                truckWidth = truckWidth.toFloat(),
                truckHeight = truckHeight.toFloat(),
                bodyColor = truckBodyColor(colors),
                wheelColor = truckWheelColor(colors),
            )
        }

        return bitmap
    }

    /** Light truck on dark plates, dark truck on light plates (matches reference). */
    private fun truckBodyColor(colors: WidgetCabinColors): Int {
        val lum = Color.luminance(colors.bg)
        return if (lum < 0.45f) 0xFFE6E1F2.toInt() else 0xFF4F2E8D.toInt()
    }

    private fun truckWheelColor(colors: WidgetCabinColors): Int {
        val lum = Color.luminance(colors.bg)
        return if (lum < 0.45f) 0xFFFFFFFF.toInt() else 0xFFF5F3FA.toInt()
    }

    /** Quiet motion dashes trailing the trailer, level with the body. */
    private fun drawSpeedLines(
        canvas: Canvas,
        truckLeadingX: Float,
        truckWidth: Float,
        barTop: Float,
        truckHeight: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE8E4FF.toInt()
            strokeCap = Paint.Cap.ROUND
        }
        val cargoLeft = truckLeadingX - truckWidth * 0.96f
        val bodyMidY = barTop - truckHeight * 0.42f
        val specs = listOf(
            Triple(-0.10f, 0.16f, 120),
            Triple(0.00f, 0.22f, 150),
            Triple(0.10f, 0.14f, 110),
        )
        specs.forEach { (yOff, lengthFrac, alpha) ->
            paint.alpha = alpha
            paint.strokeWidth = (truckHeight * 0.035f).coerceAtLeast(1f)
            val y = bodyMidY + yOff * truckHeight
            val length = truckWidth * lengthFrac
            val startX = (cargoLeft - truckWidth * 0.18f).coerceAtLeast(0f)
            canvas.drawLine(startX, y, startX + length, y, paint)
        }
    }

    /**
     * Flat side-view semi. Local origin at the truck's top-left; wheel bottoms
     * land exactly on [barTop] so the marker rides the progress track.
     */
    private fun drawFlatTruck(
        canvas: Canvas,
        leadingX: Float,
        barTop: Float,
        truckWidth: Float,
        truckHeight: Float,
        bodyColor: Int,
        wheelColor: Int,
    ) {
        val left = leadingX - truckWidth
        // Wheel bottoms = barTop  →  top of truck bitmap region = barTop - truckHeight
        val top = barTop - truckHeight
        val geom = TruckGeom(truckWidth, truckHeight)
        canvas.withSave {
            translate(left, top)

            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bodyColor
                style = Paint.Style.FILL
            }
            val wheels = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = wheelColor
                style = Paint.Style.FILL
            }

            // Body first (trailer + hitch gap + cab), then wheels on the baseline.
            drawPath(buildTrailerPath(geom), body)
            drawPath(buildHitchPath(geom), body)
            drawPath(buildCabPath(geom), body)
            drawTruckWheels(this, geom, wheels)
        }
    }

    /**
     * Side-view proportions for a compact Class-8 silhouette.
     * [wheelCy] + [wheelRadius] == [h] so bottoms sit on local y = h (= barTop).
     */
    private data class TruckGeom(val w: Float, val h: Float) {
        val wheelRadius: Float = h * 0.155f
        val wheelCy: Float = h - wheelRadius
        /** Floor of van / cab, just above the tire tops. */
        val deck: Float = wheelCy - wheelRadius * 0.12f
        val roof: Float = h * 0.08f
        val wheelXs: FloatArray = floatArrayOf(w * 0.18f, w * 0.32f, w * 0.82f)
    }

    /** Dry van with rounded top corners and a flat floor above the tires. */
    private fun buildTrailerPath(geom: TruckGeom): Path {
        val p = Path()
        val left = geom.w * 0.02f
        val right = geom.w * 0.52f
        val top = geom.roof
        val deck = geom.deck
        val rx = geom.w * 0.045f
        p.moveTo(left, deck)
        p.lineTo(left, top + rx)
        p.quadTo(left, top, left + rx, top)
        p.lineTo(right - rx, top)
        p.quadTo(right, top, right, top + rx)
        p.lineTo(right, deck)
        p.close()
        return p
    }

    /** Narrow fifth-wheel bridge between trailer and cab (visible hitch gap). */
    private fun buildHitchPath(geom: TruckGeom): Path {
        val p = Path()
        val box = RectF(
            geom.w * 0.50f,
            geom.deck - geom.h * 0.12f,
            geom.w * 0.58f,
            geom.deck,
        )
        p.addRoundRect(box, geom.w * 0.012f, geom.w * 0.012f, Path.Direction.CW)
        return p
    }

    /** Cab-over tractor: sleeper block + raked windshield + short bumper nose. */
    private fun buildCabPath(geom: TruckGeom): Path {
        val p = Path()
        val w = geom.w
        val h = geom.h
        val deck = geom.deck
        val roof = geom.roof
        val sleeperLeft = w * 0.575f
        p.moveTo(sleeperLeft, deck)
        p.lineTo(sleeperLeft, roof + h * 0.06f)
        p.quadTo(sleeperLeft, roof, sleeperLeft + w * 0.04f, roof)
        p.lineTo(w * 0.72f, roof)
        // Windshield rake
        p.lineTo(w * 0.80f, h * 0.28f)
        p.lineTo(w * 0.92f, h * 0.28f)
        // Nose / bumper down to deck
        p.lineTo(w * 0.98f, deck * 0.78f)
        p.lineTo(w * 0.98f, deck)
        p.close()
        return p
    }

    private fun drawTruckWheels(canvas: Canvas, geom: TruckGeom, wheelPaint: Paint) {
        geom.wheelXs.forEach { cx ->
            canvas.drawCircle(cx, geom.wheelCy, geom.wheelRadius, wheelPaint)
        }
    }
}
