package com.truckerload.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramBotFeaturesTest {

    @Test
    fun removeReplyKeyboard_hidesPersistentGrid() {
        val json = TelegramBotFeatures.removeReplyKeyboard()
        assertTrue(json.getBoolean("remove_keyboard"))
        assertFalse(json.has("keyboard"))
        assertFalse(json.has("is_persistent"))
    }

    @Test
    fun commandsArray_includesTheSixFormerKeyboardActions() {
        val names = buildList {
            val array = TelegramBotFeatures.commandsArray()
            for (i in 0 until array.length()) {
                add(array.getJSONObject(i).getString("command"))
            }
        }
        assertTrue(names.containsAll(listOf("help", "status", "load", "paycheck", "import", "restore")))
        assertFalse(names.contains("help_load"))
        assertFalse(names.contains("help_pay"))
    }

    @Test
    fun leftoverKeyboardButtons_stillMapToCommands() {
        assertEquals("/help", TelegramBotFeatures.menuButtonToCommand("📋 Help"))
        assertEquals("/stats", TelegramBotFeatures.menuButtonToCommand("📊 Status"))
        assertEquals("/help_load", TelegramBotFeatures.menuButtonToCommand("📦 Load"))
        assertEquals("/help_pay", TelegramBotFeatures.menuButtonToCommand("💰 Paycheck"))
        assertEquals("/import", TelegramBotFeatures.menuButtonToCommand("📥 Import"))
        assertEquals("/restore", TelegramBotFeatures.menuButtonToCommand("🔄 Restore"))
        assertTrue(TelegramBotFeatures.isMenuButtonText("📋 Помощь"))
    }

    @Test
    fun aliasCommand_mapsMenuSlashNames() {
        assertEquals("/help_load", TelegramBotFeatures.aliasCommand("/load"))
        assertEquals("/help_load", TelegramBotFeatures.aliasCommand("/load@LOAD_COUNTER_bot"))
        assertEquals("/help_pay", TelegramBotFeatures.aliasCommand("/paycheck"))
        assertEquals("/help", TelegramBotFeatures.aliasCommand("/help"))
    }
}
