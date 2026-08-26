package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetDayCaptionTest {

    @Test
    fun selectedDay_showsThatDaysGross() {
        assertEquals(
            "$441",
            WidgetDayCaption.text(
                selected = true,
                isToday = false,
                dayGross = 441.0,
                todayLabel = "сегодня",
            ),
        )
    }

    @Test
    fun todayNotSelected_showsTodayLabel() {
        assertEquals(
            "сегодня",
            WidgetDayCaption.text(
                selected = false,
                isToday = true,
                dayGross = 120.0,
                todayLabel = "сегодня",
            ),
        )
    }

    @Test
    fun selectedToday_prefersGrossOverTodayLabel() {
        assertEquals(
            "$120",
            WidgetDayCaption.text(
                selected = true,
                isToday = true,
                dayGross = 120.0,
                todayLabel = "сегодня",
            ),
        )
    }

    @Test
    fun otherDays_areEmDash() {
        assertEquals(
            "—",
            WidgetDayCaption.text(
                selected = false,
                isToday = false,
                dayGross = 80.0,
                todayLabel = "сегодня",
            ),
        )
    }
}
