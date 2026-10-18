package com.truckerload.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import com.truckerload.widget.WidgetStats

@Composable
internal fun WideBudgetContent(
    context: Context,
    shown: WidgetStats,
    week: WidgetStats,
    selectedOffset: Int,
) {
    SplitCabinBody(
        context = context,
        shown = shown,
        week = week,
        selectedOffset = selectedOffset,
        wide = true,
    )
}
