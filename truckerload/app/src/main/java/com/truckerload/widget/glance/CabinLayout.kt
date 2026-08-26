package com.truckerload.widget.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Spacing and type scale for the forest cabin widget. Full spec at 4×4; smaller sizes scale down. */
internal data class CabinLayout(
    val paddingH: Dp,
    val paddingV: Dp,
    val sectionGap: Dp,
    val financeGap: Dp,
    val ringDp: Dp,
    val headerSp: TextUnit,
    val dateSp: TextUnit,
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
    /** Circle only when the plate is tall enough; compact sizes use two text lines. */
    val showRing: Boolean,
)

internal fun cabinLayoutFor(size: DpSize): CabinLayout {
    val wide = size.width >= CabinSize4x2.width
    val tall = size.height >= CabinSize4x3.height
    val full = size.height >= CabinSize4x4.height
    if (!wide) {
        return CabinLayout(
            paddingH = 12.dp,
            paddingV = 12.dp,
            sectionGap = 0.dp,
            financeGap = 8.dp,
            ringDp = 72.dp,
            headerSp = 13.sp,
            dateSp = 10.sp,
            amountSp = 14.sp,
            percentSp = 10.sp,
            metricSp = 11.sp,
            metricGap = 4.dp,
            dayChipDp = 22.dp,
            dayCaptionSp = 8.sp,
            showDayCaptions = false,
            showDivider = false,
            actionBtnDp = 34.dp,
            actionIconDp = 16.dp,
            actionLabelSp = 10.sp,
            showActionLabels = false,
            showRing = false,
        )
    }
    if (full) {
        return CabinLayout(
            paddingH = 20.dp,
            paddingV = 20.dp,
            sectionGap = 18.dp,
            financeGap = 14.dp,
            ringDp = 92.dp,
            headerSp = 15.sp,
            dateSp = 12.sp,
            amountSp = 20.sp,
            percentSp = 11.sp,
            metricSp = 13.sp,
            metricGap = 8.dp,
            dayChipDp = 30.dp,
            dayCaptionSp = 10.sp,
            showDayCaptions = true,
            showDivider = true,
            actionBtnDp = 44.dp,
            actionIconDp = 20.dp,
            actionLabelSp = 11.sp,
            showActionLabels = true,
            showRing = true,
        )
    }
    if (tall) {
        return CabinLayout(
            paddingH = 14.dp,
            paddingV = 12.dp,
            sectionGap = 10.dp,
            financeGap = 12.dp,
            ringDp = 76.dp,
            headerSp = 14.sp,
            dateSp = 11.sp,
            amountSp = 16.sp,
            percentSp = 10.sp,
            metricSp = 12.sp,
            metricGap = 6.dp,
            dayChipDp = 26.dp,
            dayCaptionSp = 9.sp,
            showDayCaptions = true,
            showDivider = true,
            actionBtnDp = 38.dp,
            actionIconDp = 18.dp,
            actionLabelSp = 10.sp,
            showActionLabels = true,
            showRing = true,
        )
    }
    return CabinLayout(
        paddingH = 12.dp,
        paddingV = 8.dp,
        sectionGap = 6.dp,
        financeGap = 10.dp,
        ringDp = 64.dp,
        headerSp = 13.sp,
        dateSp = 10.sp,
        amountSp = 14.sp,
        percentSp = 10.sp,
        metricSp = 11.sp,
        metricGap = 4.dp,
        dayChipDp = 24.dp,
        dayCaptionSp = 8.sp,
        showDayCaptions = false,
        showDivider = false,
        actionBtnDp = 32.dp,
        actionIconDp = 16.dp,
        actionLabelSp = 10.sp,
        showActionLabels = true,
        showRing = false,
    )
}
