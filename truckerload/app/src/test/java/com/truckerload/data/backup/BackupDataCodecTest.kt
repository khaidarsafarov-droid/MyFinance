package com.truckerload.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class BackupDataCodecTest {

    @Test
    fun roundTrip_preservesLoadsPaychecksDieselAndTimestamps() {
        val original = BackupTestFixtures.sampleBackup()
        val json = BackupDataCodec.toJson(original)
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"version\":1"))
        assertTrue(!json.startsWith("\uFEFF"))

        val restored = BackupDataCodec.decode(json)
        assertEquals(original, restored)
        assertEquals(BackupTestFixtures.PARSED_AT, restored.loads[0].parsedAt)
        assertEquals(BackupTestFixtures.UPDATED_AT, restored.loads[0].updatedAt)
        assertEquals("T-116KYL6KW", restored.loads[0].tripId)
        assertEquals(2, restored.loads[0].stops.size)
        assertEquals(1, restored.loads[0].penalties.size)
        assertEquals(11, restored.loads[0].stops[0].id)
        assertEquals(original.loads[0].id, restored.loads[0].stops[0].loadId)
        assertEquals(21, restored.loads[0].penalties[0].id)
        assertEquals(original.paychecks, restored.paychecks)
        assertEquals(original.diesel, restored.diesel)
    }

    @Test
    fun toUtf8Bytes_hasNoBom() {
        val bytes = BackupDataCodec.toUtf8Bytes(BackupTestFixtures.sampleBackup())
        assertTrue(bytes[0] == '{'.code.toByte())
        val decoded = BackupDataCodec.decode(String(bytes, StandardCharsets.UTF_8))
        assertEquals(BackupTestFixtures.sampleBackup(), decoded)
    }

    @Test
    fun decode_acceptsLegacyVersionOnlyJson() {
        val json = """
            {"version":1,"exportedAt":1700000000000,"accountId":"user-abc",
             "loads":[],"paychecks":[],"diesel":[]}
        """.trimIndent()
        val restored = BackupDataCodec.decode(json)
        assertEquals(BackupSchema.V1, restored.schemaVersion)
        assertEquals(BackupSchema.V1, restored.version)
        assertEquals("user-abc", restored.accountId)
        assertEquals(1_700_000_000_000L, restored.exportedAt)
    }

    @Test
    fun decode_acceptsMissingVersionAsV1() {
        val json = """{"exportedAt":1,"loads":[],"paychecks":[],"diesel":[]}"""
        val restored = BackupDataCodec.decode(json)
        assertEquals(BackupSchema.V1, restored.schemaVersion)
    }

    @Test
    fun decode_stripsUtf8Bom() {
        val json = BackupDataCodec.toJson(BackupTestFixtures.sampleBackup())
        val withBom = "\uFEFF$json"
        val restored = BackupDataCodec.decode(withBom)
        assertEquals(BackupTestFixtures.sampleBackup(), restored)
    }

    @Test
    fun decode_rejectsGarbageAsCorrupted() {
        try {
            BackupDataCodec.decode("not-json")
            throw AssertionError("expected Corrupted")
        } catch (e: BackupRestoreException.Corrupted) {
            assertNotNull(e)
        }
        assertEquals(null, BackupDataCodec.fromJson(""))
        assertEquals(null, BackupDataCodec.fromJson("not-json"))
    }

    @Test
    fun decode_rejectsNewerSchema() {
        val json = """{"schemaVersion":99,"version":99,"exportedAt":1,"loads":[]}"""
        try {
            BackupDataCodec.decode(json)
            throw AssertionError("expected SchemaTooNew")
        } catch (e: BackupRestoreException.SchemaTooNew) {
            assertEquals(99, e.fileVersion)
        }
    }

    @Test
    fun roundTrip_preservesAccountId() {
        val original = BackupData(
            exportedAt = 1L,
            accountId = "user-abc",
            loads = emptyList(),
        )
        val restored = BackupDataCodec.fromJson(BackupDataCodec.toJson(original))
        assertNotNull(restored)
        assertEquals("user-abc", restored!!.accountId)
        assertEquals(BackupSchema.CURRENT, restored.schemaVersion)
    }
}
