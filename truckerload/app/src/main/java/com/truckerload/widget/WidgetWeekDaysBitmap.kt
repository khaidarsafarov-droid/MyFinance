package com.truckerload.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap

/** Sun–Sat chips for the green cabin mockup: selected fill, today fill, outline otherwise. */
object WidgetWeekDaysBitmap {

    fun create(
        context: Context,
        chips: List<WidgetWeekDayHelper.DayChip>,
        widthPx: Int,
        heightPx: Int,
        selectedOffset: Int? = null,
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(168)
        val safeHeight = heightPx.coerceAtLeast(24)
        val bitmap = createBitmap(safeWidth, safeHeight)
        val canvas = Canvas(bitmap)
        val count = 7
        val gap = safeWidth * 0.02f
        val cellWidth = (safeWidth - gap * (count - 1)) / count
        for (index in 0 until count) {
            val chip = chips.getOrElse(index) { placeholderChip(index) }
            drawChip(
                context = context,
                canvas = canvas,
                chip = chip,
                selected = selectedOffset == index,
                cx = index * (cellWidth + gap) + cellWidth / 2f,
                cy = safeHeight / 2f,
                cellWidth = cellWidth,
                cellHeight = safeHeight.toFloat(),
            )
        }
        return bitmap
    }

    fun createChip(
        context: Context,
        chip: WidgetWeekDayHelper.DayChip,
        selected: Boolean,
        sizePx: Int,
    ): Bitmap {
        val safe = sizePx.coerceAtLeast(24)
        val bitmap = createBitmap(safe, safe)
        drawChip(
            context = context,
            canvas = Canvas(bitmap),
            chip = chip,
            selected = selected,
            cx = safe / 2f,
            cy = safe / 2f,
            cellWidth = safe.toFloat(),
            cellHeight = safe.toFloat(),
        )
        return bitmap
    }

    private fun placeholderChip(index: Int) = WidgetWeekDayHelper.DayChip(
        label = WidgetWeekDayHelper.dayLabels.getOrElse(index) { "?" },
        date = java.time.LocalDate.now(),
        hasLoad = false,
        isToday = false,
        isFuture = true,
    )

    private fun drawChip(
        context: Context,
        canvas: Canvas,
        chip: WidgetWeekDayHelper.DayChip,
        selected: Boolean,
        cx: Float,
        cy: Float,
        cellWidth: Float,
        cellHeight: Float,
    ) {
        val circleSize = minOf(cellWidth, cellHeight)
        val radius = circleSize / 2f
        val strokeWidth = context.resources.displayMetrics.density.coerceAtLeast(1f)
        val bounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val kind = WidgetDayChipStyle.kind(chip.hasLoad, chip.isToday, selected)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT
            this.strokeWidth = strokeWidth
            color = WidgetCabinPalette.DAY_OUTLINE
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            color = WidgetDayChipStyle.letterColor(kind)
        }

        val fill = WidgetDayChipStyle.fillColor(kind)
        if (fill != null) {
            fillPaint.color = fill
            canvas.drawOval(bounds, fillPaint)
        } else {
            val inset = strokeWidth / 2f
            canvas.drawOval(
                RectF(
                    bounds.left + inset,
                    bounds.top + inset,
                    bounds.right - inset,
                    bounds.bottom - inset,
                ),
                strokePaint,
            )
        }

        textPaint.textSize = circleSize * 0.40f
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(chip.label, cx, textY, textPaint)
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
