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
    fun amountPicker_sendsLabelsATapCanDeliverAsText() {
        val json = TelegramReceiptKeyboard.amountPicker(
            amounts = listOf(4040.30, 8454.58),
            yes = "Yes, save",
            typeOwn = "I'll type it",
            cancel = "Cancel",
        ).toString()
        assertTrue(json.contains("\"one_time_keyboard\":true"))
        assertTrue(json.contains("$4,040.30"))
        assertTrue(json.contains("Yes, save"))
        assertTrue(!json.contains("inline_keyboard"))
        assertTrue(!json.contains("callback_data"))
        val action = TelegramReceiptChoice.receiptActionData(
            raw = "$4,040.30",
            yes = "Yes, save",
            typeOwn = "I'll type it",
            cancel = "Cancel",
        )
        assertEquals(TelegramReceiptKeyboard.amountCallback(4040.30), action)
        assertEquals(
            4040.30,
            TelegramReceiptKeyboard.parseAmountCallback(action!!)!!,
            0.001,
        )
    }

    @Test
    fun buttonWordsAndTypedAmount_mapToASave_aStatementDoesNot() {
        val yes = "Yes, save"
        val typeOwn = "I'll type it"
        val cancel = "Cancel"
        assertEquals(
            TelegramReceiptKeyboard.CONFIRM,
            TelegramReceiptChoice.receiptActionData(yes, yes, typeOwn, cancel),
        )
        assertEquals(
            TelegramReceiptKeyboard.TYPE_AMOUNT,
            TelegramReceiptChoice.receiptActionData(typeOwn, yes, typeOwn, cancel),
        )
        assertEquals(
            TelegramReceiptKeyboard.CANCEL,
            TelegramReceiptChoice.receiptActionData(cancel, yes, typeOwn, cancel),
        )
        assertEquals(
            "rc:amt:10199.75",
            TelegramReceiptChoice.receiptActionData("$10,199.75", yes, typeOwn, cancel),
        )
        assertEquals(
            "rc:ok",
            TelegramReceiptChoice.receiptActionData("rc:ok", yes, typeOwn, cancel),
        )
        val statement = """
            Order Total: ${'$'}2,964.77
            Total: ${'$'}12,661.99
            Deductions
        """.trimIndent()
        assertEquals(null, TelegramReceiptChoice.matchLabel(statement, yes, typeOwn, cancel))
    }
}
