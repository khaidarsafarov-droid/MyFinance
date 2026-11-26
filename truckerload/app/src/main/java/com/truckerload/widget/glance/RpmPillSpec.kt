package com.truckerload.widget.glance

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint

/**
 * RPM pill on the split header. Text tracks the goal line (12sp) and stays a
 * step larger. Horizontal size yields to the amount; vertical size stays inside
 * the amount line so the goal row does not move.
 *
 * Glance paints [androidx.glance.text.FontWeight.Medium] as bold, so the pill
 * uses the sans-serif-medium family for the semibold face.
 */
internal object RpmPillSpec {
    const val BASE_GOAL_SP = 12f
    const val BASE_TEXT_SP = 13f
    const val BASE_PAD_H_DP = 12f
    const val BASE_PAD_V_DP = 4.5f
    const val STROKE_DP = 1.25f
    const val MIN_STROKE_DP = 1f
    const val MAX_STROKE_DP = 1.5f
    const val CORNER_DP = 50f
    const val GAP_DP = 4f
    const val MIN_TEXT_SCALE = 0.72f
    const val MIN_PAD_H_DP = 6f
    const val MIN_LAYOUT_SCALE = 0.5f
    const val MEASURE_SLACK_DP = 1f
    const val TEXT_ARGB = 0xFF6EE7A0.toInt()
    const val BG_ARGB = 0xFF124030.toInt()
    const val STROKE_ARGB = 0xFF46BE78.toInt()
    const val PREVIOUS_TEXT_SP = 11f
    const val PREVIOUS_PAD_V_DP = 2f
    const val FACE = "sans-serif-medium"
}

internal data class RpmPillLayout(
    val textSp: Float,
    val padHDp: Float,
    val padVDp: Float,
    val strokeDp: Float,
    val cornerDp: Float,
    /** Set when the label must ellipsize. Null keeps the pill wrapped to the text. */
    val textMaxWidthDp: Float?,
)

internal data class RpmPillSpace(
    val rowWidthDp: Float,
    val amountWidthDp: Float,
    val amountLineHeightDp: Float,
    val measureWidthDp: (textSp: Float) -> Float,
    val measureHeightDp: (textSp: Float) -> Float,
)

internal fun resolveRpmPill(metricSp: Float, space: RpmPillSpace): RpmPillLayout {
    val scale = (metricSp / RpmPillSpec.BASE_GOAL_SP).coerceAtLeast(RpmPillSpec.MIN_LAYOUT_SCALE)
    val stroke = (RpmPillSpec.STROKE_DP * scale).coerceIn(
        RpmPillSpec.MIN_STROKE_DP,
        RpmPillSpec.MAX_STROKE_DP,
    )
    val textScale = textScaleFor(scale, stroke, space)
    val textSp = RpmPillSpec.BASE_TEXT_SP * scale * textScale
    val fitted = capToRow(scale, textScale, stroke, space)
    return RpmPillLayout(
        textSp = textSp,
        padHDp = fitted.padH,
        padVDp = verticalPadding(textSp, scale, stroke, space),
        strokeDp = stroke,
        cornerDp = RpmPillSpec.CORNER_DP,
        textMaxWidthDp = fitted.textMax,
    )
}

internal fun rpmLabelWidthDp(
    text: String,
    textSp: Float,
    bold: Boolean,
    density: Float,
    scaledDensity: Float,
): Float {
    if (textSp <= 0f || density <= 0f) return 0f
    return textPaint(textSp, bold, scaledDensity).measureText(text) / density + RpmPillSpec.MEASURE_SLACK_DP
}

internal fun rpmLabelHeightDp(
    textSp: Float,
    bold: Boolean,
    density: Float,
    scaledDensity: Float,
): Float {
    if (textSp <= 0f || density <= 0f) return 0f
    val metrics = textPaint(textSp, bold, scaledDensity).fontMetrics
    return (metrics.bottom - metrics.top) / density
}

private data class WidthFit(val padH: Float, val textMax: Float?)

private fun textScaleFor(scale: Float, stroke: Float, space: RpmPillSpace): Float {
    val available = availableWidth(space)
    val padH = RpmPillSpec.BASE_PAD_H_DP * scale
    val inner = space.measureWidthDp(RpmPillSpec.BASE_TEXT_SP * scale) + padH * 2f
    val raw = when {
        inner + stroke * 2f <= available -> 1f
        inner <= 0f -> RpmPillSpec.MIN_TEXT_SCALE
        else -> (available - stroke * 2f) / inner
    }
    return raw.coerceIn(RpmPillSpec.MIN_TEXT_SCALE, 1f)
}

private fun capToRow(scale: Float, textScale: Float, stroke: Float, space: RpmPillSpace): WidthFit {
    val available = availableWidth(space)
    val padH = RpmPillSpec.BASE_PAD_H_DP * scale * textScale
    val textWidth = space.measureWidthDp(RpmPillSpec.BASE_TEXT_SP * scale * textScale)
    if (textWidth + padH * 2f + stroke * 2f <= available) return WidthFit(padH, null)
    val minPad = (RpmPillSpec.MIN_PAD_H_DP * scale).coerceAtMost(padH)
    val minChrome = minPad * 2f + stroke * 2f
    val usedPad = if (available <= minChrome) {
        ((available - stroke * 2f) / 2f).coerceAtLeast(0f)
    } else {
        minPad
    }
    return WidthFit(usedPad, (available - usedPad * 2f - stroke * 2f).coerceAtLeast(0f))
}

private fun verticalPadding(textSp: Float, scale: Float, stroke: Float, space: RpmPillSpace): Float {
    val designed = RpmPillSpec.BASE_PAD_V_DP * scale
    val overflow = space.measureHeightDp(textSp) + designed * 2f + stroke * 2f - space.amountLineHeightDp
    return if (overflow <= 0f) designed else (designed - overflow / 2f).coerceAtLeast(0f)
}

private fun availableWidth(space: RpmPillSpace): Float =
    (space.rowWidthDp - space.amountWidthDp - RpmPillSpec.GAP_DP).coerceAtLeast(0f)

private fun textPaint(textSp: Float, bold: Boolean, scaledDensity: Float): TextPaint =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (bold) {
            Typeface.create("sans-serif", Typeface.BOLD)
        } else {
            Typeface.create(RpmPillSpec.FACE, Typeface.NORMAL)
        }
        textSize = textSp * scaledDensity
    }
