package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class TelegramMessageDateTest {

    @Test
    fun parseToMillis_dotDatetimeFromHtmlTitle() {
        val millis = TelegramMessageDate.parseToMillis("28.06.2025 14:30:00 UTC+03:00")!!
        val cal = Calendar.getInstance(Locale.US).apply { timeInMillis = millis }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH))
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun parseUnixSeconds_isoDateStringAndDateUnixtime() {
        val fromIso = TelegramMessageDate.parseUnixSeconds("2025-07-05T10:00:00")!!
        val cal = Calendar.getInstance(Locale.US).apply { timeInMillis = fromIso * 1000L }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JULY, cal.get(Calendar.MONTH))
        assertEquals(5, cal.get(Calendar.DAY_OF_MONTH))

        val unix = TelegramMessageDate.parseUnixSeconds("1751709600")
        assertEquals(1751709600L, unix)
        assertEquals(1751709600L, TelegramMessageDate.parseUnixSeconds(1751709600L))
    }

    @Test
    fun scanChatHistory_copyHeadersAnchorFollowingTrip() {
        val text = """
            bruce, [05.07.2025 10:00]
            Trip ID: T-OLD
            body

            bruce, [21.08.2025 02:09]
            Trip ID: T-AUG
            body
        """.trimIndent()
        val headers = TelegramMessageDate.scanChatHistory(text)
        assertTrue(headers.size >= 2)
        val oldStart = text.indexOf("Trip ID: T-OLD")
        val augStart = text.indexOf("Trip ID: T-AUG")
        val oldRef = TelegramMessageDate.referenceMillisAt(headers, oldStart, fallback = 0L)
        val augRef = TelegramMessageDate.referenceMillisAt(headers, augStart, fallback = 0L)
        val oldCal = Calendar.getInstance(Locale.US).apply { timeInMillis = oldRef }
        val augCal = Calendar.getInstance(Locale.US).apply { timeInMillis = augRef }
        assertEquals(2025, oldCal.get(Calendar.YEAR))
        assertEquals(Calendar.JULY, oldCal.get(Calendar.MONTH))
        assertEquals(5, oldCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(2025, augCal.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, augCal.get(Calendar.MONTH))
        assertEquals(21, augCal.get(Calendar.DAY_OF_MONTH))
    }
}
