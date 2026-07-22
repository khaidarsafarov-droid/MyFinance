package com.truckerload.data.backup

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
}
