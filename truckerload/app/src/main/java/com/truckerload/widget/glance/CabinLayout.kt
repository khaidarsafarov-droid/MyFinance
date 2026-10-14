package com.truckerload.widget.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.widget.WidgetSizeMode

/** Spacing and type scale for mockup-style cabin widget (progress bar, not ring). */
internal data class CabinLayout(
    val paddingH: Dp,
    val paddingV: Dp,
    val sectionGap: Dp,
    val financeGap: Dp,
    val progressBarDp: Dp,
    val progressBarHeadroomDp: Dp,
    val headerSp: TextUnit,
    val dateSp: TextUnit,
    val revenueLabelSp: TextUnit,
    val amountSp: TextUnit,
    val percentSp: TextUnit,
    val metricSp: TextUnit,
    val metricGap: Dp,
    val dayChipDp: Dp,
    val dayCaptionSp: TextUnit,
    val showWeekRow: Boolean,
    val showDayCaptions: Boolean,
    /** Goal, diesel, and take-home. Small cells show revenue and the goal only. */
    val showDieselNet: Boolean,
    /** RPM sits with the week numbers only when the cell is tall enough. */
    val showRpm: Boolean,
    val actionBtnDp: Dp,
    val actionIconDp: Dp,
    val bucket: CabinBucket,
)

internal enum class CabinBucket { MICRO, SHORT, COMPACT, TALL, FULL, SQUARE }

/**
 * Leaves a few dp so font padding and the launcher's rounded clip do not
 * shave the action row. Leftover height is distributed by [cabinSectionGap].
 */
private val FitSafety = 8.dp

internal fun cabinLayoutFor(
    size: DpSize,
    sizeMode: WidgetSizeMode = WidgetSizeMode.AUTO,
): CabinLayout {
    val wide = size.width >= CabinSize4x2.width
    val budget = (size.height - FitSafety).coerceAtLeast(1.dp)
    val base = if (wide) widestThatFits(budget, sizeMode) else squareThatFits(budget)
    val grow = sizeMode == WidgetSizeMode.AUTO || sizeMode == WidgetSizeMode.LARGE
    return if (grow) inflate(base, budget, wide) else base
}

internal fun cabinBucket(
    size: DpSize,
    sizeMode: WidgetSizeMode = WidgetSizeMode.AUTO,
): CabinBucket = cabinLayoutFor(size, sizeMode).bucket

/**
 * Fixed stack height for [layout]. Weighted gaps are not included, so this is
 * the minimum cell height that still shows every row, including the actions.
 */
internal fun cabinFixedHeight(layout: CabinLayout, wide: Boolean): Dp {
    val header = textLine(layout.headerSp)
    val progress = layout.progressBarDp + layout.progressBarHeadroomDp
    val rows = metricRowCount(layout)
    val metricBlock = textLine(layout.metricSp) * rows +
        layout.metricGap * (rows - 1).coerceAtLeast(0)
    val stats = if (wide) {
        maxOf(
            textLine(layout.revenueLabelSp) + textLine(layout.amountSp),
            metricBlock,
        )
    } else {
        textLine(layout.amountSp) + metricBlock
    }
    val days = if (wide && layout.showWeekRow) {
        7.dp + layout.dayChipDp +
            if (layout.showDayCaptions) 4.dp + textLine(layout.dayCaptionSp) else 0.dp
    } else {
        0.dp
    }
    val gaps = layout.sectionGap * gapCount(wide, layout.showWeekRow)
    return layout.paddingV * 2 + header + progress + stats + days + layout.actionBtnDp + gaps
}

private fun widestThatFits(budget: Dp, sizeMode: WidgetSizeMode): CabinLayout {
    val cap = when (sizeMode) {
        WidgetSizeMode.SMALL -> CabinBucket.COMPACT
        WidgetSizeMode.MEDIUM -> CabinBucket.TALL
        WidgetSizeMode.LARGE, WidgetSizeMode.AUTO -> CabinBucket.FULL
    }
    return wideCandidates(cap).firstOrNull { cabinFixedHeight(it, wide = true) <= budget }
        ?: microWideLayout()
}

private fun wideCandidates(cap: CabinBucket): List<CabinLayout> = when (cap) {
    CabinBucket.COMPACT, CabinBucket.MICRO, CabinBucket.SHORT -> listOf(
        compactWideLayout(),
        shortWideLayout(),
        microWideLayout(),
    )
    CabinBucket.TALL -> listOf(
        tallWideLayout(),
        compactWideLayout(),
        shortWideLayout(),
        microWideLayout(),
    )
    CabinBucket.FULL, CabinBucket.SQUARE -> listOf(
        fullWideLayout(),
        tallWideLayout(),
        compactWideLayout(),
        shortWideLayout(),
        microWideLayout(),
    )
}

private fun squareThatFits(budget: Dp): CabinLayout {
    val full = squareLayout()
    return if (cabinFixedHeight(full, wide = false) <= budget) full else squareTightLayout()
}

private fun inflate(base: CabinLayout, budget: Dp, wide: Boolean): CabinLayout {
    if (budget - cabinFixedHeight(base, wide) < 28.dp) return base
    var best = base
    var scale = 1.06f
    while (scale <= 1.48f) {
        val grown = base.scaled(scale)
        if (cabinFixedHeight(grown, wide) > budget - 8.dp) break
        best = grown
        scale += 0.06f
    }
    return best
}

private fun CabinLayout.scaled(scale: Float): CabinLayout {
    fun Dp.grow(factor: Float = scale) = this * factor
    fun TextUnit.grow() = this * scale
    return copy(
        paddingH = paddingH.grow(scale.coerceAtMost(1.15f)),
        paddingV = paddingV.grow(scale.coerceAtMost(1.08f)),
        financeGap = financeGap.grow(scale.coerceAtMost(1.15f)),
        progressBarDp = progressBarDp.grow(scale.coerceAtMost(1.2f)),
        progressBarHeadroomDp = progressBarHeadroomDp.grow(scale.coerceAtMost(1.12f)),
        headerSp = headerSp.grow(),
        dateSp = dateSp.grow(),
        revenueLabelSp = revenueLabelSp.grow(),
        amountSp = amountSp.grow(),
        percentSp = percentSp.grow(),
        metricSp = metricSp.grow(),
        metricGap = metricGap.grow(scale.coerceAtMost(1.15f)),
        dayChipDp = dayChipDp.grow(),
        dayCaptionSp = dayCaptionSp.grow(),
        actionBtnDp = actionBtnDp.grow(scale.coerceAtMost(1.3f)),
        actionIconDp = actionIconDp.grow(scale.coerceAtMost(1.3f)),
    )
}

private fun gapCount(wide: Boolean, showWeekRow: Boolean): Int =
    if (wide && showWeekRow) 4 else 3

/** Goal is always shown. Diesel and take-home, then RPM, are added as the cell grows. */
private fun metricRowCount(layout: CabinLayout): Int {
    var rows = 1
    if (layout.showDieselNet) rows += 2
    if (layout.showRpm) rows += 1
    return rows
}

/** Glance text keeps font padding, so a line is taller than the sp size. */
private fun textLine(sp: TextUnit): Dp = (sp.value * 1.35f).dp + 3.dp

private fun maxOf(a: Dp, b: Dp): Dp = if (a >= b) a else b

private fun squareLayout() = CabinLayout(
    paddingH = 10.dp,
    paddingV = 8.dp,
    sectionGap = 4.dp,
    financeGap = 6.dp,
    progressBarDp = 8.dp,
    progressBarHeadroomDp = 16.dp,
    headerSp = 13.sp,
    dateSp = 12.sp,
    revenueLabelSp = 12.sp,
    amountSp = 20.sp,
    percentSp = 12.sp,
    metricSp = 12.sp,
    metricGap = 2.dp,
    dayChipDp = 20.dp,
    dayCaptionSp = 12.sp,
    showWeekRow = false,
    showDayCaptions = false,
    showDieselNet = false,
    showRpm = false,
    actionBtnDp = 32.dp,
    actionIconDp = 18.dp,
    bucket = CabinBucket.SQUARE,
)

private fun squareTightLayout() = CabinLayout(
    paddingH = 8.dp,
    paddingV = 2.dp,
    sectionGap = 1.dp,
    financeGap = 2.dp,
    progressBarDp = 6.dp,
    progressBarHeadroomDp = 8.dp,
    headerSp = 12.sp,
    dateSp = 12.sp,
    revenueLabelSp = 12.sp,
    amountSp = 16.sp,
    percentSp = 12.sp,
    metricSp = 12.sp,
    metricGap = 1.dp,
    dayChipDp = 16.dp,
    dayCaptionSp = 12.sp,
    showWeekRow = false,
    showDayCaptions = false,
    showDieselNet = false,
    showRpm = false,
    actionBtnDp = 24.dp,
    actionIconDp = 14.dp,
    bucket = CabinBucket.SQUARE,
)

private fun microWideLayout() = CabinLayout(
    paddingH = 10.dp,
    paddingV = 2.dp,
    sectionGap = 1.dp,
    financeGap = 2.dp,
    progressBarDp = 6.dp,
    progressBarHeadroomDp = 8.dp,
    headerSp = 12.sp,
    dateSp = 12.sp,
    revenueLabelSp = 12.sp,
    amountSp = 16.sp,
    percentSp = 12.sp,
    metricSp = 12.sp,
    metricGap = 1.dp,
    dayChipDp = 16.dp,
    dayCaptionSp = 12.sp,
    showWeekRow = false,
    showDayCaptions = false,
    showDieselNet = false,
    showRpm = false,
    actionBtnDp = 24.dp,
    actionIconDp = 14.dp,
    bucket = CabinBucket.MICRO,
)

private fun shortWideLayout() = CabinLayout(
    paddingH = 12.dp,
    paddingV = 6.dp,
    sectionGap = 2.dp,
    financeGap = 4.dp,
    progressBarDp = 8.dp,
    progressBarHeadroomDp = 16.dp,
    headerSp = 12.sp,
    dateSp = 12.sp,
    revenueLabelSp = 12.sp,
    amountSp = 20.sp,
    percentSp = 12.sp,
    metricSp = 12.sp,
    metricGap = 2.dp,
    dayChipDp = 20.dp,
    dayCaptionSp = 12.sp,
    showWeekRow = true,
    showDayCaptions = false,
    showDieselNet = false,
    showRpm = false,
    actionBtnDp = 30.dp,
    actionIconDp = 18.dp,
    bucket = CabinBucket.SHORT,
)

private fun compactWideLayout() = CabinLayout(
    paddingH = 12.dp,
    paddingV = 8.dp,
    sectionGap = 4.dp,
    financeGap = 8.dp,
    progressBarDp = 10.dp,
    progressBarHeadroomDp = 20.dp,
    headerSp = 13.sp,
    dateSp = 12.sp,
    revenueLabelSp = 12.sp,
    amountSp = 22.sp,
    percentSp = 12.sp,
    metricSp = 13.sp,
    metricGap = 2.dp,
    dayChipDp = 22.dp,
    dayCaptionSp = 12.sp,
    showWeekRow = true,
    showDayCaptions = false,
    showDieselNet = true,
    showRpm = false,
    actionBtnDp = 36.dp,
    actionIconDp = 22.dp,
    bucket = CabinBucket.COMPACT,
)

private fun tallWideLayout() = CabinLayout(
    paddingH = 14.dp,
    paddingV = 8.dp,
    sectionGap = 4.dp,
    financeGap = 8.dp,
    progressBarDp = 11.dp,
    progressBarHeadroomDp = 18.dp,
    headerSp = 14.sp,
    dateSp = 12.sp,
    revenueLabelSp = 12.sp,
    amountSp = 24.sp,
    percentSp = 12.sp,
    metricSp = 13.sp,
    metricGap = 2.dp,
    dayChipDp = 26.dp,
    dayCaptionSp = 12.sp,
    showWeekRow = true,
    showDayCaptions = true,
    showDieselNet = true,
    showRpm = true,
    actionBtnDp = 40.dp,
    actionIconDp = 24.dp,
    bucket = CabinBucket.TALL,
)

private fun fullWideLayout() = CabinLayout(
    paddingH = 16.dp,
    paddingV = 4.dp,
    sectionGap = 4.dp,
    financeGap = 8.dp,
    progressBarDp = 12.dp,
    progressBarHeadroomDp = 14.dp,
    headerSp = 15.sp,
    dateSp = 12.sp,
    revenueLabelSp = 12.sp,
    amountSp = 26.sp,
    percentSp = 12.sp,
    metricSp = 12.sp,
    metricGap = 1.dp,
    dayChipDp = 28.dp,
    dayCaptionSp = 10.sp,
    showWeekRow = true,
    showDayCaptions = true,
    showDieselNet = true,
    showRpm = true,
    actionBtnDp = 42.dp,
    actionIconDp = 26.dp,
    bucket = CabinBucket.FULL,
)
