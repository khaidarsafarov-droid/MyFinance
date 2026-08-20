package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.truckerload.R

/** Draws a circular goal-progress ring for App Widget [android.widget.RemoteViews]. */
object WidgetProgressRingBitmap {

    fun create(
        context: Context,
        progressPercent: Float,
        sizePx: Int,
        progressColor: Int,
    ): Bitmap = create(
        progressPercent = progressPercent,
        sizePx = sizePx,
        progressColor = progressColor,
        trackColor = WidgetThemeColors.surfaceVariant(context),
    )

    fun create(
        progressPercent: Float,
        sizePx: Int,
        progressColor: Int,
        trackColor: Int,
    ): Bitmap {
        val safeSize = sizePx.coerceAtLeast(48)
        val bitmap = createBitmap(safeSize, safeSize)
        val canvas = Canvas(bitmap)
        val stroke = (safeSize * 0.13f).coerceIn(safeSize * 0.10f, safeSize * 0.16f)
        val inset = stroke / 2f + 1.5f
        val arcBounds = RectF(inset, inset, safeSize - inset, safeSize - inset)

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
                color = progressColor
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

    @ColorRes
    fun progressColorResForStatus(
        paceStatus: String,
        goalMet: Boolean,
        daysRemaining: Int = Int.MAX_VALUE,
    ): Int = when {
        goalMet || paceStatus == "GOAL_MET" || paceStatus == "AHEAD" -> R.color.widget_success
        // On-track is healthy — Success, not Warning (yellow would look like a problem).
        paceStatus == "ON_TRACK" -> R.color.widget_success
        paceStatus == "BEHIND" && daysRemaining <= 1 -> R.color.widget_rpm_bad
        paceStatus == "BEHIND" -> R.color.widget_rpm_warn
        else -> R.color.widget_primary
    }

    fun progressColorForStatus(
        context: Context,
        paceStatus: String,
        goalMet: Boolean,
        daysRemaining: Int = Int.MAX_VALUE,
    ): Int = ContextCompat.getColor(
        context,
        progressColorResForStatus(paceStatus, goalMet, daysRemaining),
    )
}
