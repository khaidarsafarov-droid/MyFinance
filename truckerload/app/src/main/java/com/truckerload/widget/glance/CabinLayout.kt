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
    val showDayCaptions: Boolean,
    val showDivider: Boolean,
    val actionBtnDp: Dp,
    val actionIconDp: Dp,
    val actionLabelSp: TextUnit,
    val showActionLabels: Boolean,
    val showQuickActions: Boolean,
)

internal enum class CabinBucket { SQUARE, COMPACT, TALL, FULL }

internal fun cabinLayoutFor(
    size: DpSize,
    sizeMode: WidgetSizeMode = WidgetSizeMode.AUTO,
): CabinLayout = when (cabinBucket(size, sizeMode)) {
    CabinBucket.SQUARE -> squareLayout()
    CabinBucket.COMPACT -> compactWideLayout()
    CabinBucket.TALL -> tallWideLayout()
    CabinBucket.FULL -> fullWideLayout()
}

internal fun cabinBucket(
    size: DpSize,
    sizeMode: WidgetSizeMode = WidgetSizeMode.AUTO,
): CabinBucket {
    val wide = size.width >= CabinSize4x2.width
    val fromSize = when {
        !wide -> CabinBucket.SQUARE
        size.height >= CabinSize4x4.height -> CabinBucket.FULL
        size.height >= CabinSize4x3.height -> CabinBucket.TALL
        else -> CabinBucket.COMPACT
    }
    return when (sizeMode) {
        WidgetSizeMode.SMALL -> if (wide) CabinBucket.COMPACT else CabinBucket.SQUARE
        WidgetSizeMode.MEDIUM -> when {
            !wide -> CabinBucket.SQUARE
            fromSize == CabinBucket.COMPACT -> CabinBucket.COMPACT
            else -> CabinBucket.TALL
        }
        WidgetSizeMode.LARGE, WidgetSizeMode.AUTO -> fromSize
    }
}

private fun squareLayout() = CabinLayout(
    paddingH = 10.dp,
    paddingV = 10.dp,
    sectionGap = 4.dp,
    financeGap = 6.dp,
    progressBarDp = 8.dp,
    progressBarHeadroomDp = 10.dp,
    headerSp = 11.sp,
    dateSp = 9.sp,
    revenueLabelSp = 9.sp,
    amountSp = 13.sp,
    percentSp = 9.sp,
    metricSp = 10.sp,
    metricGap = 3.dp,
    dayChipDp = 20.dp,
    dayCaptionSp = 7.sp,
    showDayCaptions = false,
    showDivider = false,
    actionBtnDp = 28.dp,
    actionIconDp = 14.dp,
    actionLabelSp = 9.sp,
    showActionLabels = false,
    showQuickActions = true,
)

private fun compactWideLayout() = CabinLayout(
    paddingH = 12.dp,
    paddingV = 8.dp,
    sectionGap = 4.dp,
    financeGap = 8.dp,
    progressBarDp = 9.dp,
    progressBarHeadroomDp = 12.dp,
    headerSp = 13.sp,
    dateSp = 10.sp,
    revenueLabelSp = 9.sp,
    amountSp = 18.sp,
    percentSp = 10.sp,
    metricSp = 11.sp,
    metricGap = 3.dp,
    dayChipDp = 22.dp,
    dayCaptionSp = 8.sp,
    showDayCaptions = false,
    showDivider = false,
    actionBtnDp = 34.dp,
    actionIconDp = 16.dp,
    actionLabelSp = 9.sp,
    showActionLabels = true,
    showQuickActions = true,
)

private fun tallWideLayout() = CabinLayout(
    paddingH = 14.dp,
    paddingV = 10.dp,
    sectionGap = 6.dp,
    financeGap = 10.dp,
    progressBarDp = 10.dp,
    progressBarHeadroomDp = 14.dp,
    headerSp = 14.sp,
    dateSp = 11.sp,
    revenueLabelSp = 10.sp,
    amountSp = 22.sp,
    percentSp = 10.sp,
    metricSp = 12.sp,
    metricGap = 4.dp,
    dayChipDp = 26.dp,
    dayCaptionSp = 9.sp,
    showDayCaptions = true,
    showDivider = false,
    actionBtnDp = 38.dp,
    actionIconDp = 18.dp,
    actionLabelSp = 10.sp,
    showActionLabels = true,
    showQuickActions = true,
)

private fun fullWideLayout() = CabinLayout(
    paddingH = 16.dp,
    paddingV = 12.dp,
    sectionGap = 8.dp,
    financeGap = 12.dp,
    progressBarDp = 11.dp,
    progressBarHeadroomDp = 16.dp,
    headerSp = 15.sp,
    dateSp = 12.sp,
    revenueLabelSp = 11.sp,
    amountSp = 26.sp,
    percentSp = 11.sp,
    metricSp = 13.sp,
    metricGap = 6.dp,
    dayChipDp = 28.dp,
    dayCaptionSp = 10.sp,
    showDayCaptions = true,
    showDivider = false,
    actionBtnDp = 42.dp,
    actionIconDp = 20.dp,
    actionLabelSp = 11.sp,
    showActionLabels = true,
    showQuickActions = true,
)
