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
internal fun goalLineSp(
    text: String,
    availableDp: Float,
    defaultSp: Float,
    density: Float,
    scaledDensity: Float,
): Float {
    if (text.isEmpty() || availableDp <= 1f || defaultSp <= 0f || density <= 0f) return defaultSp
    val budget = (availableDp - SAFETY_DP).coerceAtLeast(1f)
    val atDefault = goalLineWidthDp(text, defaultSp, density, scaledDensity)
    if (atDefault <= budget) return defaultSp
    var sp = defaultSp * budget / atDefault
    repeat(4) {
        val width = goalLineWidthDp(text, sp, density, scaledDensity)
        if (width <= budget) return sp
        sp *= budget / width
    }
    val width = goalLineWidthDp(text, sp, density, scaledDensity)
    if (width > budget && width > 0f) sp *= budget / width
    return sp.coerceAtLeast(1f)
}

internal fun goalLineWidthDp(
    text: String,
    textSp: Float,
    density: Float,
    scaledDensity: Float,
): Float {
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
    val gaps = (layout.actionCount - 1).coerceAtLeast(0) * 8f
    val actions = layout.actionCount * layout.actionBtnDp.value + gaps
    val outer = layout.paddingH.value * 2f
    return (widgetWidthDp - outer - 8f - actions - inner).coerceAtLeast(1f)
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
