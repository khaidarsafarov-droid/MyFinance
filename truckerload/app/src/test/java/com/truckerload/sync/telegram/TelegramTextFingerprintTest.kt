package com.truckerload.sync.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramTextFingerprintTest {

    @Test
    fun sha256Hex_isStableAndHex() {
        val a = TelegramTextFingerprint.sha256Hex("diesel $45.00 Pilot")
        val b = TelegramTextFingerprint.sha256Hex("diesel $45.00 Pilot")
        assertEquals(a, b)
        assertEquals(64, a.length)
        assertTrue(a.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun dieselFingerprint_saltSeparatesDefFromDiesel() {
        val text = "DEF 12 gal $48"
        val diesel = TelegramTextFingerprint.dieselFingerprint(text)
        val def = TelegramTextFingerprint.dieselFingerprint(text, salt = "DEF")
        assertNotEquals(diesel, def)
    }
}
