package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.truckerload.R

/** Renders Sun–Sat floating soft chips for App Widget [RemoteViews]. */
object WidgetWeekDaysBitmap {

    fun create(
        context: Context,
        statuses: List<WidgetWeekDayHelper.DayStatus>,
        labels: List<String>,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(168)
        val safeHeight = heightPx.coerceAtLeast(24)
        val bitmap = createBitmap(safeWidth, safeHeight)
        val canvas = Canvas(bitmap)

        val count = 7
        val gap = safeWidth * 0.02f
        val cellWidth = (safeWidth - gap * (count - 1)) / count
        val baseCircleSize = minOf(cellWidth * 0.82f, safeHeight * 0.78f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val blueStart = ContextCompat.getColor(context, R.color.widget_primary)
        val purpleEnd = ContextCompat.getColor(context, R.color.widget_secondary)
        val futureFill = ContextCompat.getColor(context, R.color.widget_day_future)
        val labelOnAccent = Color.WHITE
        val labelOnFuture = ContextCompat.getColor(context, R.color.widget_text_hint)
        val shadowColor = Color.argb(36, 100, 116, 139)

        for (index in 0 until count) {
            val status = statuses.getOrElse(index) { WidgetWeekDayHelper.DayStatus.FUTURE }
            val label = labels.getOrElse(index) { "?" }
            val left = index * (cellWidth + gap)
            val cx = left + cellWidth / 2f
            val cy = safeHeight / 2f

            val circleSize = when (status) {
                WidgetWeekDayHelper.DayStatus.TODAY -> baseCircleSize * 1.08f
                else -> baseCircleSize
            }
            val radius = circleSize / 2f
            val bounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = shadowColor
            }
            canvas.drawOval(
                RectF(bounds.left, bounds.top + 2f, bounds.right, bounds.bottom + 3f),
                shadowPaint,
            )

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            when (status) {
                WidgetWeekDayHelper.DayStatus.PAST,
                WidgetWeekDayHelper.DayStatus.TODAY -> {
                    fillPaint.shader = LinearGradient(
                        bounds.left, bounds.top, bounds.right, bounds.bottom,
                        blueStart, purpleEnd,
                        Shader.TileMode.CLAMP,
                    )
                    textPaint.color = labelOnAccent
                    textPaint.textSize = circleSize * 0.38f
                }
                WidgetWeekDayHelper.DayStatus.FUTURE -> {
                    fillPaint.color = futureFill
                    textPaint.color = labelOnFuture
                    textPaint.textSize = circleSize * 0.40f
                }
            }

            canvas.drawOval(bounds, fillPaint)

            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, cx, textY, textPaint)
        }

        return bitmap
    }

    fun columnWidthPx(
        context: Context,
        appWidgetId: Int,
        horizontalPaddingDp: Int,
        ringWidthDp: Int,
        columnGapDp: Int,
    ): Int {
        val density = context.resources.displayMetrics.density
        val paddingPx = (horizontalPaddingDp * density * 2).toInt()
        val ringPx = (ringWidthDp * density).toInt()
        val gapPx = (columnGapDp * density).toInt()
        val minWidthDp = if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
            250
        } else {
            val options = android.appwidget.AppWidgetManager.getInstance(context)
                .getAppWidgetOptions(appWidgetId)
            options.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        }
        return ((minWidthDp * density) - paddingPx - ringPx - gapPx).toInt().coerceAtLeast(72)
    }

    fun rowWidthPx(context: Context, appWidgetId: Int, horizontalPaddingDp: Int): Int {
        val density = context.resources.displayMetrics.density
        val paddingPx = (horizontalPaddingDp * density * 2).toInt()
        val minWidthDp = if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
            250
        } else {
            val options = android.appwidget.AppWidgetManager.getInstance(context)
                .getAppWidgetOptions(appWidgetId)
            options.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        }
        return ((minWidthDp * density) - paddingPx).toInt().coerceAtLeast(168)
    }

    fun rowHeightPx(context: Context, compact: Boolean): Int {
        val dp = if (compact) 28 else 34
        return (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
    }
}
