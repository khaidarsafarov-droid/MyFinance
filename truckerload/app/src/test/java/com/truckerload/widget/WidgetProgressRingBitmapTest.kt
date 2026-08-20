package com.truckerload.widget

import com.truckerload.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetProgressRingBitmapTest {

    @Test
    fun progressColorResForStatus_mapsPaceToSemanticTokens() {
        assertEquals(
            R.color.widget_success,
            WidgetProgressRingBitmap.progressColorResForStatus("GOAL_MET", goalMet = false),
        )
        assertEquals(
            R.color.widget_success,
            WidgetProgressRingBitmap.progressColorResForStatus("AHEAD", goalMet = false),
        )
        assertEquals(
            R.color.widget_success,
            WidgetProgressRingBitmap.progressColorResForStatus("BEHIND", goalMet = true),
        )
        assertEquals(
            R.color.widget_rpm_warn,
            WidgetProgressRingBitmap.progressColorResForStatus("ON_TRACK", goalMet = false),
        )
        assertEquals(
            R.color.widget_rpm_bad,
            WidgetProgressRingBitmap.progressColorResForStatus("BEHIND", goalMet = false),
        )
        assertEquals(
            R.color.widget_primary,
            WidgetProgressRingBitmap.progressColorResForStatus("", goalMet = false),
        )
    }
}
