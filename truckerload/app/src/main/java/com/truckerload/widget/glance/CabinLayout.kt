package com.truckerload.widget.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.widget.WidgetSizeMode

/**
 * Split-header widget scale. [bucket] is the richest layout whose reserved
 * height still fits, so FULL is reachable on a 4×3 cell (~300dp) and TALL
 * is reachable just below that instead of being taller than FULL.
 */
internal data class CabinLayout(
    val paddingH: Dp,
    val paddingV: Dp,
    val sectionGap: Dp,
    val cornerDp: Dp,
    val headerSp: TextUnit,
    val amountSp: TextUnit,
    val metricSp: TextUnit,
    val headerPadDp: Dp,
    val progressBarDp: Dp,
    val progressBlockDp: Dp,
    val dayBarDp: Dp,
    val dayAmountSp: TextUnit,
    val dayCaptionSp: TextUnit,
    val showWeekRow: Boolean,
    val showDayAmounts: Boolean,
    val showDayLabels: Boolean,
    val showRpm: Boolean,
    val showRemaining: Boolean,
    /** Percent / goal line drawn inside the header (micro and square). */
    val headerMetric: Boolean,
    val actionBtnDp: Dp,
    val actionIconDp: Dp,
    val showActionLabels: Boolean,
    val actionCount: Int,
    val bucket: CabinBucket,
)

internal enum class CabinBucket { MICRO, SHORT, COMPACT, TALL, FULL, SQUARE }

/** Wide vs square. A 4-column cell is at least 250dp; 2×2 stays under that. */
internal val CabinWideMinWidth = 250.dp

/** Launcher clip and font padding. Kept small so a 236dp cell can reach FULL. */
private val FitSafety = 4.dp

internal fun cabinLayoutFor(
    size: DpSize,
    sizeMode: WidgetSizeMode = WidgetSizeMode.AUTO,
): CabinLayout {
    val wide = size.width >= CabinWideMinWidth
    val budget = (size.height - FitSafety).coerceAtLeast(1.dp)
    val base = if (wide) widestThatFits(budget, sizeMode, size.width) else squareThatFits(budget, size.width)
    val grow = sizeMode == WidgetSizeMode.AUTO || sizeMode == WidgetSizeMode.LARGE
    return if (grow) inflate(base, budget, wide) else base
}

internal fun cabinBucket(
    size: DpSize,
    sizeMode: WidgetSizeMode = WidgetSizeMode.AUTO,
): CabinBucket = cabinLayoutFor(size, sizeMode).bucket

internal fun cabinFixedHeight(layout: CabinLayout, wide: Boolean): Dp {
    val barInHeader = if (layout.progressBlockDp == 0.dp && layout.progressBarDp > 0.dp) {
        layout.progressBarDp
    } else {
        0.dp
    }
    val header = textLineOrZero(layout.headerSp) +
        textLine(layout.amountSp) +
        layout.headerPadDp +
        (if (layout.headerMetric) textLine(layout.metricSp) else 0.dp) +
        barInHeader
    if (layout.bucket == CabinBucket.MICRO) {
        return layout.paddingV * 2 + maxOf(header, layout.actionBtnDp)
    }
    val days = if (wide && layout.showWeekRow) {
        (if (layout.showDayAmounts) textLine(layout.dayAmountSp) else 0.dp) +
            layout.dayBarDp +
            (if (layout.showDayLabels) textLine(layout.dayCaptionSp) else 0.dp)
    } else {
        0.dp
    }
    val gaps = layout.sectionGap * gapCount(layout)
    return layout.paddingV * 2 + header + layout.progressBlockDp + days + layout.actionBtnDp + gaps
}

private fun widestThatFits(budget: Dp, sizeMode: WidgetSizeMode, width: Dp): CabinLayout {
    val cap = when (sizeMode) {
        WidgetSizeMode.SMALL -> CabinBucket.COMPACT
        WidgetSizeMode.MEDIUM -> CabinBucket.TALL
        WidgetSizeMode.LARGE, WidgetSizeMode.AUTO -> CabinBucket.FULL
    }
    return wideCandidates(cap, width).firstOrNull { cabinFixedHeight(it, wide = true) <= budget }
        ?: microWideLayout(width)
}

private fun wideCandidates(cap: CabinBucket, width: Dp): List<CabinLayout> {
    val all = listOf(
        fullWideLayout(width),
        tallWideLayout(width),
        compactWideLayout(width),
        shortWideLayout(width),
        microWideLayout(width),
    )
    val capped = all.filter { rank(it.bucket) <= rank(cap) }
    return capped.ifEmpty { listOf(microWideLayout(width)) }
}

private fun squareThatFits(budget: Dp, width: Dp): CabinLayout {
    val full = squareLayout(width)
    return if (cabinFixedHeight(full, wide = false) <= budget) full else squareTightLayout(width)
}

private fun inflate(base: CabinLayout, budget: Dp, wide: Boolean): CabinLayout {
    if (budget - cabinFixedHeight(base, wide) < 28.dp) return base
    var best = base
    var scale = 1.06f
    while (scale <= 1.36f) {
        val grown = base.scaled(scale)
        if (cabinFixedHeight(grown, wide) > budget - 4.dp) break
        best = grown
        scale += 0.06f
    }
    return best
}

private fun CabinLayout.scaled(scale: Float): CabinLayout {
    fun Dp.grow(factor: Float = scale) = this * factor
    fun TextUnit.grow() = this * scale
    return copy(
        paddingH = paddingH.grow(scale.coerceAtMost(1.12f)),
        paddingV = paddingV.grow(scale.coerceAtMost(1.08f)),
        headerSp = headerSp.grow(),
        amountSp = amountSp.grow(),
        metricSp = metricSp.grow(),
        headerPadDp = headerPadDp.grow(scale.coerceAtMost(1.08f)),
        progressBarDp = progressBarDp.grow(scale.coerceAtMost(1.2f)),
        progressBlockDp = progressBlockDp.grow(scale.coerceAtMost(1.15f)),
        dayBarDp = dayBarDp.grow(),
        dayAmountSp = dayAmountSp.grow(),
        dayCaptionSp = dayCaptionSp.grow(),
        actionBtnDp = maxOf(actionBtnDp.grow(scale.coerceAtMost(1.15f)), 48.dp),
        actionIconDp = actionIconDp.grow(scale.coerceAtMost(1.15f)),
    )
}

private fun gapCount(layout: CabinLayout): Int {
    if (layout.bucket == CabinBucket.MICRO) return 0
    var sections = 2 // header + actions
    if (layout.progressBlockDp > 0.dp) sections++
    if (layout.showWeekRow) sections++
    return sections - 1
}

private fun rank(bucket: CabinBucket): Int = when (bucket) {
    CabinBucket.MICRO -> 0
    CabinBucket.SHORT -> 1
    CabinBucket.COMPACT -> 2
    CabinBucket.TALL -> 3
    CabinBucket.FULL -> 4
    CabinBucket.SQUARE -> 4
}

private fun textLine(sp: TextUnit): Dp = (sp.value * 1.35f).dp + 3.dp

private fun textLineOrZero(sp: TextUnit): Dp = if (sp.value <= 0f) 0.dp else textLine(sp)

private fun maxOf(a: Dp, b: Dp): Dp = if (a >= b) a else b

private fun base(
    width: Dp,
    bucket: CabinBucket,
    paddingH: Dp,
    paddingV: Dp,
    sectionGap: Dp,
    cornerDp: Dp,
    headerSp: TextUnit,
    amountSp: TextUnit,
    metricSp: TextUnit,
    headerPadDp: Dp,
    progressBarDp: Dp,
    progressBlockDp: Dp,
    dayBarDp: Dp,
    showWeekRow: Boolean,
    showDayAmounts: Boolean,
    showDayLabels: Boolean,
    showRpm: Boolean,
    showRemaining: Boolean,
    headerMetric: Boolean,
    showActionLabels: Boolean,
): CabinLayout = CabinLayout(
    paddingH = paddingH,
    paddingV = paddingV,
    sectionGap = sectionGap,
    cornerDp = cornerDp,
    headerSp = headerSp,
    amountSp = amountSp,
    metricSp = metricSp,
    headerPadDp = headerPadDp,
    progressBarDp = progressBarDp,
    progressBlockDp = progressBlockDp,
    dayBarDp = dayBarDp,
    dayAmountSp = 10.sp,
    dayCaptionSp = 11.sp,
    showWeekRow = showWeekRow,
    showDayAmounts = showDayAmounts,
    showDayLabels = showDayLabels,
    showRpm = showRpm,
    showRemaining = showRemaining,
    headerMetric = headerMetric,
    actionBtnDp = 48.dp,
    actionIconDp = 22.dp,
    showActionLabels = showActionLabels && width >= 300.dp,
    actionCount = CabinQuickAction.countFor(width),
    bucket = bucket,
)

private fun microWideLayout(width: Dp) = base(
    width = width,
    bucket = CabinBucket.MICRO,
    paddingH = 10.dp,
    paddingV = 6.dp,
    sectionGap = 0.dp,
    cornerDp = 28.dp,
    headerSp = 12.sp,
    amountSp = 22.sp,
    metricSp = 12.sp,
    headerPadDp = 8.dp,
    progressBarDp = 6.dp,
    progressBlockDp = 0.dp,
    dayBarDp = 0.dp,
    showWeekRow = false,
    showDayAmounts = false,
    showDayLabels = false,
    showRpm = false,
    showRemaining = false,
    headerMetric = true,
    showActionLabels = false,
)

private fun shortWideLayout(width: Dp) = base(
    width = width,
    bucket = CabinBucket.SHORT,
    paddingH = 12.dp,
    paddingV = 4.dp,
    sectionGap = 2.dp,
    cornerDp = 28.dp,
    headerSp = 12.sp,
    amountSp = 22.sp,
    metricSp = 12.sp,
    headerPadDp = 6.dp,
    progressBarDp = 6.dp,
    progressBlockDp = 6.dp,
    dayBarDp = 18.dp,
    showWeekRow = true,
    showDayAmounts = false,
    showDayLabels = false,
    showRpm = false,
    showRemaining = false,
    headerMetric = false,
    showActionLabels = false,
)

private fun compactWideLayout(width: Dp) = base(
    width = width,
    bucket = CabinBucket.COMPACT,
    paddingH = 12.dp,
    paddingV = 6.dp,
    sectionGap = 4.dp,
    cornerDp = 28.dp,
    headerSp = 12.sp,
    amountSp = 26.sp,
    metricSp = 12.sp,
    headerPadDp = 8.dp,
    progressBarDp = 6.dp,
    progressBlockDp = 0.dp,
    dayBarDp = 28.dp,
    showWeekRow = true,
    showDayAmounts = false,
    showDayLabels = true,
    showRpm = false,
    showRemaining = false,
    headerMetric = false,
    showActionLabels = true,
)

private fun tallWideLayout(width: Dp) = base(
    width = width,
    bucket = CabinBucket.TALL,
    paddingH = 14.dp,
    paddingV = 6.dp,
    sectionGap = 4.dp,
    cornerDp = 28.dp,
    headerSp = 12.sp,
    amountSp = 26.sp,
    metricSp = 12.sp,
    headerPadDp = 8.dp,
    progressBarDp = 6.dp,
    progressBlockDp = 22.dp,
    dayBarDp = 28.dp,
    showWeekRow = true,
    showDayAmounts = false,
    showDayLabels = true,
    showRpm = true,
    showRemaining = false,
    headerMetric = false,
    showActionLabels = true,
)

private fun fullWideLayout(width: Dp) = base(
    width = width,
    bucket = CabinBucket.FULL,
    paddingH = 14.dp,
    paddingV = 6.dp,
    sectionGap = 4.dp,
    cornerDp = 28.dp,
    headerSp = 12.sp,
    amountSp = 28.sp,
    metricSp = 12.sp,
    headerPadDp = 8.dp,
    progressBarDp = 8.dp,
    progressBlockDp = 28.dp,
    dayBarDp = 28.dp,
    showWeekRow = true,
    showDayAmounts = true,
    showDayLabels = true,
    showRpm = true,
    showRemaining = true,
    headerMetric = false,
    showActionLabels = true,
)

private fun squareLayout(width: Dp) = base(
    width = width,
    bucket = CabinBucket.SQUARE,
    paddingH = 10.dp,
    paddingV = 8.dp,
    sectionGap = 6.dp,
    cornerDp = 24.dp,
    headerSp = 12.sp,
    amountSp = 22.sp,
    metricSp = 12.sp,
    headerPadDp = 8.dp,
    progressBarDp = 6.dp,
    progressBlockDp = 0.dp,
    dayBarDp = 0.dp,
    showWeekRow = false,
    showDayAmounts = false,
    showDayLabels = false,
    showRpm = false,
    showRemaining = false,
    headerMetric = true,
    showActionLabels = false,
)

private fun squareTightLayout(width: Dp) = base(
    width = width,
    bucket = CabinBucket.SQUARE,
    paddingH = 8.dp,
    paddingV = 4.dp,
    sectionGap = 2.dp,
    cornerDp = 24.dp,
    headerSp = 0.sp,
    amountSp = 16.sp,
    metricSp = 11.sp,
    headerPadDp = 2.dp,
    progressBarDp = 4.dp,
    progressBlockDp = 0.dp,
    dayBarDp = 0.dp,
    showWeekRow = false,
    showDayAmounts = false,
    showDayLabels = false,
    showRpm = false,
    showRemaining = false,
    headerMetric = true,
    showActionLabels = false,
)
