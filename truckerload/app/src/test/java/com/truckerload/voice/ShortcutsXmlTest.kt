package com.truckerload.voice

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutsXmlTest {

    @Test
    fun shortcutsDeclareJournalAppActionsWithoutActionsXml() {
        val xml = readRes("xml/shortcuts.xml")
        assertTrue(xml.contains("custom.actions.intent.ADD_DIESEL"))
        assertTrue(xml.contains("custom.actions.intent.ADD_PAYCHECK"))
        assertTrue(xml.contains("custom.actions.intent.QUERY_WEEKLY_GROSS"))
        assertTrue(xml.contains("actions.intent.GET_THING"))
        assertTrue(xml.contains("actions.intent.OPEN_APP_FEATURE"))
        assertTrue(xml.contains("truckerload://assistant/add_diesel"))
        assertTrue(xml.contains("truckerload://assistant/add_paycheck"))
        assertTrue(xml.contains("truckerload://assistant/weekly_gross"))
        assertFalse(File("src/main/res/xml/actions.xml").isFile)
        assertFalse(File("app/src/main/res/xml/actions.xml").isFile)
    }

    private fun readRes(relativePath: String): String {
        val candidates = listOf(
            File("src/main/res/$relativePath"),
            File("app/src/main/res/$relativePath"),
            File("../app/src/main/res/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Resource not found: $relativePath")
    }
}
