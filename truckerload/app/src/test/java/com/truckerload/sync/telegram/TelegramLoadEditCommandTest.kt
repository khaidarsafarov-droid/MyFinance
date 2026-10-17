package com.truckerload.sync.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramLoadEditCommandTest {

    @Test
    fun undoAndRate_areRecognized() {
        assertTrue(TelegramLoadEditCommand.isUndo("/undo"))
        assertTrue(TelegramLoadEditCommand.isUndo("/undo@TruckoRigBot"))
        assertEquals(2500.0, TelegramLoadEditCommand.amount("/rate 2500")!!, 0.001)
        assertEquals(2500.5, TelegramLoadEditCommand.amount("/rate@bot $2,500.50")!!, 0.001)
        assertNull(TelegramLoadEditCommand.amount("/rate"))
        assertNull(TelegramLoadEditCommand.amount("rate 10"))
    }
}
