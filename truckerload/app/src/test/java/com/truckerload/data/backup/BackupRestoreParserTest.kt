package com.truckerload.data.backup

import com.truckerload.utils.BackupNoteFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class BackupRestoreParserTest {

    @Test
    fun parse_jsonExport_roundTripsPayload() {
        val backup = BackupTestFixtures.sampleBackup()
        val bytes = BackupDataCodec.toUtf8Bytes(backup)
        val json = BackupRestoreParser.parseToJson(bytes).getOrThrow()
        assertEquals(backup, BackupDataCodec.decode(json))
    }

    @Test
    fun parse_jsonWithUtf8Bom_roundTripsPayload() {
        val backup = BackupTestFixtures.sampleBackup()
        val json = BackupDataCodec.toJson(backup)
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            json.toByteArray(StandardCharsets.UTF_8)
        val parsed = BackupRestoreParser.parseToJson(bytes).getOrThrow()
        assertEquals(backup, BackupDataCodec.decode(parsed))
    }

    @Test
    fun parse_legacyEmbeddedBlock_succeeds() {
        val json = """{"version":1,"exportedAt":1,"loads":[],"paychecks":[],"diesel":[]}"""
        val content = """
            Visible note text
            -----BEGIN TRUCKERLOAD BACKUP-----
            $json
            -----END TRUCKERLOAD BACKUP-----
        """.trimIndent()
        val parsed = BackupRestoreParser.parseToJson(content.toByteArray(StandardCharsets.UTF_8))
            .getOrThrow()
        val restored = BackupDataCodec.decode(parsed)
        assertEquals(BackupSchema.V1, restored.schemaVersion)
        assertTrue(restored.loads.isEmpty())
    }

    @Test
    fun parse_visibleOnlyChartNote_isChartNoteError() {
        val note = BackupNoteFormatter.buildNote(BackupTestFixtures.sampleBackup()).visibleText
        val result = BackupRestoreParser.parseToJson(note.toByteArray(StandardCharsets.UTF_8))
        assertTrue(
            "old Loads_*.txt chart must not parse as backup JSON",
            result.exceptionOrNull() is BackupRestoreException.ChartNoteNotBackup,
        )
    }

    @Test
    fun parse_garbage_isInvalidFormat() {
        val result = BackupRestoreParser.parseToJson("hello world".toByteArray())
        assertTrue(result.exceptionOrNull() is BackupRestoreException.InvalidFormat)
    }
}
