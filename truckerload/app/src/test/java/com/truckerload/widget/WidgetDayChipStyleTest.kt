package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetDayChipStyleTest {

    @Test
    fun today_usesTodayFillEvenWhenSelectedOrHasLoad() {
        assertEquals(
            WidgetDayChipStyle.Kind.TODAY,
            WidgetDayChipStyle.kind(hasLoad = true, isToday = true, selected = true),
        )
        assertEquals(WidgetCabinPalette.DAY_TODAY, WidgetDayChipStyle.fillColor(WidgetDayChipStyle.Kind.TODAY))
        assertEquals(WidgetCabinPalette.ON_FILLED, WidgetDayChipStyle.letterColor(WidgetDayChipStyle.Kind.TODAY))
    }

    @Test
    fun pastDayWithDataOrSelection_isFilledForest() {
        assertEquals(
            WidgetDayChipStyle.Kind.FILLED,
            WidgetDayChipStyle.kind(hasLoad = true, isToday = false, selected = false),
        )
        assertEquals(
            WidgetDayChipStyle.Kind.FILLED,
            WidgetDayChipStyle.kind(hasLoad = false, isToday = false, selected = true),
        )
        assertEquals(WidgetCabinPalette.DAY_FILLED, WidgetDayChipStyle.fillColor(WidgetDayChipStyle.Kind.FILLED))
    }

    @Test
    fun futureOrEmptyPast_isOutline() {
        assertEquals(
            WidgetDayChipStyle.Kind.OUTLINE,
            WidgetDayChipStyle.kind(hasLoad = false, isToday = false, selected = false),
        )
        assertNull(WidgetDayChipStyle.fillColor(WidgetDayChipStyle.Kind.OUTLINE))
        assertEquals(
            WidgetCabinPalette.DAY_FUTURE_LETTER,
            WidgetDayChipStyle.letterColor(WidgetDayChipStyle.Kind.OUTLINE),
        )
    }
}
