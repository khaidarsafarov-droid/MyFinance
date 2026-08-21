package com.truckerload.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveApiClientTest {
    @Test
    fun multipartRelated_containsMetadataAndPayload() {
        val body = GoogleDriveApiClient.buildMultipartRelated(
            boundary = "bnd",
            metadataJson = """{"name":"truckerload_backup.tlb"}""",
            fileJson = """{"version":1,"loads":[]}""",
        )
        assertTrue(body.contains("--bnd"))
        assertTrue(body.contains("truckerload_backup.tlb"))
        assertTrue(body.contains("\"version\":1"))
        assertTrue(body.endsWith("--bnd--") || body.trimEnd().endsWith("--bnd--"))
    }

    @Test
    fun driveUploadBody_isUnwrappedCodecJson() {
        val backup = BackupTestFixtures.sampleBackup()
        val json = BackupDataCodec.toJson(backup)
        val body = GoogleDriveApiClient.buildMultipartRelated(
            boundary = "bnd",
            metadataJson = """{"name":"truckerload_backup_user.tlb"}""",
            fileJson = json,
        )
        assertTrue(body.contains(json))
        assertTrue(body.contains("application/json; charset=UTF-8"))
        val restored = BackupDataCodec.decode(json)
        assertEquals(backup, restored)
        assertEquals(backup.loads[0].stops, restored.loads[0].stops)
        assertEquals(backup.paychecks, restored.paychecks)
        assertEquals(backup.diesel, restored.diesel)
    }
}
