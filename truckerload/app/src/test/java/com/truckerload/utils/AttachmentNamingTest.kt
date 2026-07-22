package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AttachmentNamingTest {

    @Test
    fun buildFileName_usesTripIdAndDate() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.JULY, 21, 14, 30, 52)
            set(Calendar.MILLISECOND, 0)
        }
        val name = AttachmentNaming.buildFileName(
            tripId = "114RS221Y",
            loadDate = "2026-07-21",
            timestamp = cal.timeInMillis,
            extension = "jpg",
        )
        assertTrue(name.startsWith("114RS221Y_2026-07-21_"))
        assertTrue(name.endsWith(".jpg"))
    }

    @Test
    fun sanitize_stripsUnsafeCharacters() {
        assertEquals("T-116KYL6KW", AttachmentNaming.sanitize("T-116KYL6KW"))
        assertEquals("Trip_ID", AttachmentNaming.sanitize("Trip ID"))
        assertEquals("file", AttachmentNaming.sanitize("???"))
        assertEquals("a_b", AttachmentNaming.sanitize("a/b"))
    }
}
