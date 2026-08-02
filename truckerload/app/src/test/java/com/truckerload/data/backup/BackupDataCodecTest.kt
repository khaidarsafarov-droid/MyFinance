package com.truckerload.data.backup

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDataCodecTest {

    @Test
    fun roundTrip_preservesActualFinishDateAndCounts() {
        val load = Load(
            id = "id-1",
            tripId = "T-116",
            date = "2026-07-17",
            totalRate = 2500.0,
            totalMiles = 850.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 1,
            weekNumber = 29,
            year = 2026,
            rawMessage = "raw",
            parsedAt = 10L,
            updatedAt = 20L,
            actualFinishDate = "2026-07-18",
            durationDays = 2.0,
            pace = 1250.0,
        )
        val original = BackupData(
            version = 1,
            exportedAt = 1_700_000_000_000L,
            loads = listOf(load),
        )
        val json = BackupDataCodec.toJson(original)
        val restored = BackupDataCodec.fromJson(json)
        assertNotNull(restored)
        assertEquals(1, restored!!.loads.size)
        assertEquals("2026-07-18", restored.loads[0].actualFinishDate)
        assertEquals(2500.0, restored.loads[0].totalRate, 0.001)
        assertEquals(1_700_000_000_000L, restored.exportedAt)
        assertTrue(json.contains("actualFinishDate"))
    }

    @Test
    fun fromJson_rejectsGarbage() {
        assertEquals(null, BackupDataCodec.fromJson("not-json"))
        assertEquals(null, BackupDataCodec.fromJson(""))
    }

    @Test
    fun roundTrip_preservesAccountId() {
        val original = BackupData(
            version = 1,
            exportedAt = 1L,
            accountId = "user-abc",
            loads = emptyList(),
        )
        val restored = BackupDataCodec.fromJson(BackupDataCodec.toJson(original))
        assertNotNull(restored)
        assertEquals("user-abc", restored!!.accountId)
    }
}
