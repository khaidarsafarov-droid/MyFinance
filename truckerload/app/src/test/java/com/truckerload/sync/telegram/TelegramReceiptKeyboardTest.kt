package com.truckerload.sync.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramReceiptKeyboardTest {

    @Test
    fun confirmKeyboard_savesOnlyOnOkCallback() {
        val json = TelegramReceiptKeyboard.confirm("Yes, save", "No").toString()
        assertTrue(json.contains(TelegramReceiptKeyboard.CONFIRM))
        assertTrue(json.contains(TelegramReceiptKeyboard.CANCEL))
        assertTrue(!json.contains(TelegramReceiptKeyboard.LOAD))
        assertEquals("rc:ok", TelegramReceiptKeyboard.CONFIRM)
        assertTrue(TelegramReceiptKeyboard.isReceiptCallback(TelegramReceiptKeyboard.CONFIRM))
    }
}
