package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import androidx.core.graphics.createBitmap
import com.truckerload.R

/** Draws a Material You circular goal-progress ring for App Widget [RemoteViews]. */
object WidgetProgressRingBitmap {

    fun create(
        context: Context,
        progressPercent: Float,
        sizePx: Int,
        @Suppress("UNUSED_PARAMETER") progressColor: Int,
    ): Bitmap {
        val safeSize = sizePx.coerceAtLeast(48)
        val bitmap = createBitmap(safeSize, safeSize)
        val canvas = Canvas(bitmap)
        val trackColor = WidgetThemeColors.surfaceVariant(context)
        val primary = WidgetThemeColors.primary(context)
        val primaryContainer = WidgetThemeColors.primaryContainer(context)
        val stroke = (safeSize * 0.06f).coerceIn(4f, 14f)
        val inset = stroke / 2f + 2f
        val arcBounds = RectF(inset, inset, safeSize - inset, safeSize - inset)
        val cx = safeSize / 2f
        val cy = safeSize / 2f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = stroke
            color = trackColor
            strokeCap = Paint.Cap.BUTT
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
                    intArrayOf(primary, primaryContainer, primary),
                    floatArrayOf(0f, 0.45f, 1f),
                )
            }
            val sweep = (360f * (progress / 100f)).coerceAtMost(359.6f)
            canvas.drawArc(arcBounds, -90f, sweep, false, progressPaint)
        }

        return bitmap
    }

    fun ringSizePx(context: Context, compact: Boolean, expanded: Boolean, splitLayout: Boolean = false): Int {
        val res = context.resources
        val dp = when {
            expanded -> res.getDimension(R.dimen.widget_circle_expanded) / res.displayMetrics.density
            compact -> res.getDimension(R.dimen.widget_circle_compact) / res.displayMetrics.density
            splitLayout -> res.getDimension(R.dimen.widget_circle_standard_split) / res.displayMetrics.density
            else -> res.getDimension(R.dimen.widget_circle_standard) / res.displayMetrics.density
        }
        return (dp * res.displayMetrics.density).toInt().coerceAtLeast(48)
    }

    fun progressColorForStatus(context: Context, paceStatus: String, goalMet: Boolean): Int =
        when {
            goalMet -> WidgetThemeColors.tertiary(context)
            paceStatus == "BEHIND" -> WidgetThemeColors.error(context)
            else -> WidgetThemeColors.primary(context)
        }
}
