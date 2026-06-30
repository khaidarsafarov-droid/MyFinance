package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.ContextCompat
import com.truckerload.R

/** Segmented and continuous progress bars for App Widget [RemoteViews]. */
object WidgetSegmentedProgressBitmap {

    fun createWeekSegments(
        context: Context,
        filledSegments: Int,
        totalSegments: Int,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(48)
        val safeHeight = heightPx.coerceAtLeast(6)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val trackColor = ContextCompat.getColor(context, R.color.widget_progress_track)
        val accentStart = ContextCompat.getColor(context, R.color.widget_primary)
        val accentEnd = ContextCompat.getColor(context, R.color.widget_secondary)
        val total = totalSegments.coerceAtLeast(1)
        val filled = filledSegments.coerceIn(0, total)
        val gapPx = (safeHeight * 0.35f).coerceAtLeast(2f)
        val segmentWidth = (safeWidth - gapPx * (total - 1)) / total
        val corner = safeHeight / 2f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (index in 0 until total) {
            val left = index * (segmentWidth + gapPx)
            val rect = RectF(left, 0f, left + segmentWidth, safeHeight.toFloat())
            if (index < filled) {
                fillPaint.shader = LinearGradient(
                    left,
                    0f,
                    left + segmentWidth,
                    0f,
                    accentStart,
                    accentEnd,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(rect, corner, corner, fillPaint)
                fillPaint.shader = null
            } else {
                trackPaint.color = trackColor
                canvas.drawRoundRect(rect, corner, corner, trackPaint)
            }
        }
        return bitmap
    }

    fun createGrossProgress(
        context: Context,
        progressPercent: Float,
        targetMarkerPercent: Float,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(48)
        val safeHeight = heightPx.coerceAtLeast(6)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val trackColor = ContextCompat.getColor(context, R.color.widget_progress_track)
        val accentStart = ContextCompat.getColor(context, R.color.widget_primary)
        val accentEnd = ContextCompat.getColor(context, R.color.widget_secondary)
        val markerColor = ContextCompat.getColor(context, R.color.widget_text_secondary)
        val corner = safeHeight / 2f
        val bounds = RectF(0f, 0f, safeWidth.toFloat(), safeHeight.toFloat())

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
        canvas.drawRoundRect(bounds, corner, corner, trackPaint)

        val progress = progressPercent.coerceIn(0f, 100f)
        if (progress > 0f) {
            val fillWidth = safeWidth * (progress / 100f)
            val fillRect = RectF(0f, 0f, fillWidth, safeHeight.toFloat())
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    fillWidth.coerceAtLeast(1f),
                    0f,
                    accentStart,
                    accentEnd,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(fillRect, corner, corner, fillPaint)
        }

        val marker = targetMarkerPercent.coerceIn(0f, 100f)
        if (marker > 0f && marker < 100f) {
            val markerX = safeWidth * (marker / 100f)
            val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = markerColor
                strokeWidth = (safeHeight * 0.18f).coerceAtLeast(1.5f)
            }
            canvas.drawLine(markerX, 0f, markerX, safeHeight.toFloat(), markerPaint)
        }

        return bitmap
    }

    fun barWidthPx(context: Context, appWidgetId: Int, horizontalPaddingDp: Int): Int {
        val density = context.resources.displayMetrics.density
        val paddingPx = (horizontalPaddingDp * density * 2).toInt()
        val minWidthDp = if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
            250
        } else {
            val options = android.appwidget.AppWidgetManager.getInstance(context)
                .getAppWidgetOptions(appWidgetId)
            options.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        }
        return ((minWidthDp * density) - paddingPx).toInt().coerceAtLeast(120)
    }

    fun barHeightPx(context: Context, compact: Boolean): Int {
        val dp = if (compact) 6 else 8
        return (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(6)
    }
}
