package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDayCaptionTest {

    @Test
    fun dayWithEarnings_showsGross() {
        assertEquals(
            "$441",
            WidgetDayCaption.text(
                isFuture = false,
                isToday = false,
                dayGross = 441.0,
                todayLabel = "сегодня",
            ),
        )
    }

    @Test
    fun today_showsTodayLabelEvenWithEarnings() {
        assertEquals(
            "сегодня",
            WidgetDayCaption.text(
                isFuture = false,
                isToday = true,
                dayGross = 120.0,
                todayLabel = "сегодня",
            ),
        )
    }

    @Test
    fun futureDay_isEmDash() {
        assertEquals(
            "—",
            WidgetDayCaption.text(
                isFuture = true,
                isToday = false,
                dayGross = 80.0,
                todayLabel = "сегодня",
            ),
        )
        assertTrue(
            WidgetDayCaption.usesEmptyColor(
                isFuture = true,
                isToday = false,
                dayGross = 80.0,
            ),
        )
    }

    @Test
    fun pastDayWithoutEarnings_isEmDash() {
        assertEquals(
            "—",
            WidgetDayCaption.text(
                isFuture = false,
                isToday = false,
                dayGross = 0.0,
                todayLabel = "сегодня",
            ),
        )
        assertTrue(
            WidgetDayCaption.usesEmptyColor(
                isFuture = false,
                isToday = false,
                dayGross = 0.0,
            ),
        )
    }

    @Test
    fun pastDayWithEarnings_doesNotUseEmptyColor() {
        assertFalse(
            WidgetDayCaption.usesEmptyColor(
                isFuture = false,
                isToday = false,
                dayGross = 80.0,
            ),
        )
    }
}
