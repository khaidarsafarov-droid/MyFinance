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

    @Test
    fun amountPicker_usesAmountCallbacksAndTypeOwn() {
        val json = TelegramReceiptKeyboard.amountPicker(
            amounts = listOf(4040.30, 8454.58),
            yes = "Yes, save",
            typeOwn = "I'll type it",
            cancel = "Cancel",
        ).toString()
        assertTrue(json.contains(TelegramReceiptKeyboard.amountCallback(4040.30)))
        assertTrue(json.contains(TelegramReceiptKeyboard.TYPE_AMOUNT))
        assertTrue(json.contains(TelegramReceiptKeyboard.CANCEL))
        assertEquals(
            4040.30,
            TelegramReceiptKeyboard.parseAmountCallback("rc:amt:4040.30")!!,
            0.001,
        )
    }
}
