package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import androidx.core.content.ContextCompat
import com.truckerload.R

/** Draws a soft UI circular goal-progress ring for App Widget [RemoteViews]. */
object WidgetProgressRingBitmap {

    fun create(
        context: Context,
        progressPercent: Float,
        sizePx: Int,
        @Suppress("UNUSED_PARAMETER") progressColor: Int,
    ): Bitmap {
        val safeSize = sizePx.coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val trackColor = ContextCompat.getColor(context, R.color.widget_progress_track)
        val blue = ContextCompat.getColor(context, R.color.widget_primary)
        val purple = ContextCompat.getColor(context, R.color.widget_secondary)
        val stroke = (safeSize * 0.06f).coerceIn(4f, 14f)
        val inset = stroke / 2f + 2f
        val arcBounds = RectF(inset, inset, safeSize - inset, safeSize - inset)
        val cx = safeSize / 2f
        val cy = safeSize / 2f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = stroke
            color = trackColor
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint)

        val progress = progressPercent.coerceIn(0f, 100f)
        if (progress > 0f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                shader = SweepGradient(
                    cx,
                    cy,
                    intArrayOf(blue, purple, blue),
                    floatArrayOf(0f, 0.5f, 1f),
                )
            }
            val sweep = 360f * (progress / 100f)
            canvas.drawArc(arcBounds, -90f, sweep, false, progressPaint)
        }

        return bitmap
    }

    fun ringSizePx(context: Context, compact: Boolean, expanded: Boolean, splitLayout: Boolean = false): Int {
        val dp = when {
            expanded -> 160
            compact -> 64
            splitLayout -> 88
            else -> 120
        }
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt().coerceAtLeast(48)
    }

    fun progressColorForStatus(context: Context, paceStatus: String, goalMet: Boolean): Int =
        when {
            goalMet -> ContextCompat.getColor(context, R.color.widget_green)
            paceStatus == "BEHIND" -> ContextCompat.getColor(context, R.color.widget_secondary)
            else -> ContextCompat.getColor(context, R.color.widget_progress_fill)
        }
}
