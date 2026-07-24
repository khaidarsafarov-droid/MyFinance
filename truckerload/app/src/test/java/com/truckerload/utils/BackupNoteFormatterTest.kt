package com.truckerload.utils

import com.truckerload.data.backup.BackupData
import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupNoteFormatterTest {

    @Test
    fun buildNote_sortsLoadsByDateAndFormatsVisibleLines() {
        val backup = BackupData(
            loads = listOf(
                load(id = "2", tripId = "T-LATE", date = "2026-07-23", rate = 2500.0),
                load(id = "1", tripId = "T-EARLY", date = "2026-07-22", rate = 1250.5),
            )
        )

        val note = BackupNoteFormatter.buildNote(backup)
        val lines = note.visibleText.lines()

        assertEquals(2, note.loadCount)
        assertEquals("22.07.2026 | T-EARLY | ${'$'}1,250.50 | Charlotte, NC -> Nashville, TN", lines[0])
        assertEquals("23.07.2026 | T-LATE | ${'$'}2,500.00 | Charlotte, NC -> Nashville, TN", lines[1])
    }

    @Test
    fun buildNote_usesLoadIdWhenTripIdBlank() {
        val note = BackupNoteFormatter.buildNote(
            BackupData(loads = listOf(load(id = "LOAD-ID", tripId = "")))
        )

        assertTrue(note.visibleText.contains("LOAD-ID"))
    }

    @Test
    fun extractBackupJson_returnsPlainJsonContent() {
        val json = """{"version":1,"loads":[]}"""

        assertEquals(json, BackupNoteFormatter.extractBackupJson("  $json  "))
    }

    @Test
    fun extractBackupJson_returnsLegacyBlockPayload() {
        val json = """{"version":1}"""
        val content = """
            Visible note text
            -----BEGIN TRUCKERLOAD BACKUP-----
            $json
            -----END TRUCKERLOAD BACKUP-----
        """.trimIndent()

        assertEquals(json, BackupNoteFormatter.extractBackupJson(content))
    }

    @Test
    fun extractBackupJson_returnsNullForVisibleOnlyNote() {
        assertNull(BackupNoteFormatter.extractBackupJson("22.07.2026 | T-1 | ${'$'}100.00 | A -> B"))
    }

    @Test
    fun noteFileNamesUseTxtAndCompanionSuffixes() {
        val txtName = BackupNoteFormatter.noteFileName(1_700_000_000_000L)

        assertTrue(txtName.startsWith("Loads_"))
        assertTrue(txtName.endsWith(".txt"))
        assertEquals(txtName.removeSuffix(".txt") + ".tlb", BackupNoteFormatter.companionFileName(txtName))
    }

    private fun load(
        id: String,
        tripId: String,
        date: String = "2026-07-22",
        rate: Double = 100.0,
    ): Load = Load(
        id = id,
        tripId = tripId,
        date = date,
        totalRate = rate,
        totalMiles = 100.0,
        pointA = "Charlotte, NC",
        pointB = "Nashville, TN",
        puCount = 1,
        delCount = 1,
        weekNumber = 30,
        year = 2026,
        rawMessage = "Trip ID: $tripId",
        parsedAt = 1L,
        updatedAt = 1L,
    )
}
