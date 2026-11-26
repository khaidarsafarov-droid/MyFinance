package com.truckerload.widget.glance

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * Keeps a goal-percent line inside its cell. The default size is unchanged
 * when the string already fits; only a long value such as "245% de l'objectif"
 * steps down. Glance multiplies sp by fontScale, so measurement uses scaled density.
 */
internal fun goalLineSp(text: String, widthDp: Float, defaultSp: Float, density: Float, scaled: Float): Float {
    if (!canMeasure(text, widthDp, defaultSp, density)) return defaultSp
    val budget = (widthDp - SAFETY_DP).coerceAtLeast(MIN_TEXT_SP)
    val atDefault = goalLineWidthDp(text, defaultSp, density, scaled)
    return if (atDefault <= budget) {
        defaultSp
    } else {
        shrinkSp(text, defaultSp * budget / atDefault, budget, density, scaled)
    }
}

private fun canMeasure(text: String, widthDp: Float, defaultSp: Float, density: Float): Boolean =
    text.isNotEmpty() && widthDp > MIN_TEXT_SP && defaultSp > 0f && density > 0f

private fun shrinkSp(text: String, startSp: Float, budget: Float, density: Float, scaledDensity: Float): Float {
    var sp = startSp
    repeat(FIT_PASSES) {
        val width = goalLineWidthDp(text, sp, density, scaledDensity)
        if (width <= budget) return sp
        sp *= budget / width
    }
    return finishShrink(text, sp, budget, density, scaledDensity)
}

private fun finishShrink(text: String, sp: Float, budget: Float, density: Float, scaledDensity: Float): Float {
    val width = goalLineWidthDp(text, sp, density, scaledDensity)
    val fitted = if (width > budget && width > 0f) sp * budget / width else sp
    return fitted.coerceAtLeast(MIN_TEXT_SP)
}

internal fun goalLineWidthDp(text: String, textSp: Float, density: Float, scaledDensity: Float): Float {
    if (textSp <= 0f || density <= 0f) return 0f
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textSize = textSp * scaledDensity
    }
    return paint.measureText(text) / density
}

/** Inner width of the percent line. Micro shares the row with the action buttons. */
internal fun percentLineWidthDp(layout: CabinLayout, widgetWidthDp: Float): Float {
    val inner = layout.paddingH.value * 2f
    if (layout.bucket != CabinBucket.MICRO) {
        return (widgetWidthDp - inner).coerceAtLeast(1f)
    }
    val gaps = (layout.actionCount - 1).coerceAtLeast(0) * ACTION_GAP_DP
    val actions = layout.actionCount * layout.actionBtnDp.value + gaps
    val outer = layout.paddingH.value * 2f
    return (widgetWidthDp - outer - MICRO_SPACER_DP - actions - inner).coerceAtLeast(MIN_TEXT_SP)
}

@Composable
internal fun FittedGoalLine(
    context: Context,
    text: String,
    argb: Int,
    defaultSp: Float,
    availableDp: Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    val metrics = context.resources.displayMetrics
    val scaled = metrics.density * context.resources.configuration.fontScale
    val sp = goalLineSp(text, availableDp, defaultSp, metrics.density, scaled)
    Text(
        text = text,
        maxLines = 1,
        modifier = modifier,
        style = TextStyle(
            color = cabinColor(argb),
            fontSize = sp.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily("sans-serif"),
        ),
    )
}

private const val SAFETY_DP = 4f
private const val MIN_TEXT_SP = 1f
private const val FIT_PASSES = 4
private const val ACTION_GAP_DP = 8f
private const val MICRO_SPACER_DP = 8f
