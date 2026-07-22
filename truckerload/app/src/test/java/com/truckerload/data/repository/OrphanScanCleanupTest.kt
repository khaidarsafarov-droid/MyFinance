package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.domain.model.Load
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OrphanScanCleanupTest {

    private lateinit var db: AppDatabase
    private lateinit var loadRepo: LoadRepository
    private lateinit var scanRepo: ScanRepository
    private lateinit var scansDir: File

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        loadRepo = LoadRepository(db)
        scanRepo = ScanRepository(db)
        scansDir = File(context.cacheDir, "scans_orphan_test").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        db.close()
        scansDir.deleteRecursively()
    }

    @Test
    fun cleanupOrphanScanFiles_deletesUnreferencedPdfs() = runBlocking {
        val kept = File(scansDir, "kept.pdf").apply { writeText("%PDF-kept") }
        val orphan = File(scansDir, "orphan.pdf").apply { writeText("%PDF-orphan") }
        db.scanDao().insert(
            ScanEntity(
                id = "scan-kept",
                fileName = kept.name,
                filePath = kept.absolutePath,
                timestamp = 1L,
                fileSizeBytes = 8,
                pageCount = 1,
                ocrText = "",
                loadId = null,
            ),
        )

        val removed = scanRepo.cleanupOrphanScanFiles(scansDir)

        assertEquals(1, removed)
        assertTrue(kept.exists())
        assertFalse(orphan.exists())
    }

    @Test
    fun cleanupOrphanAttachments_removesScanRowsForMissingLoad() = runBlocking {
        val load = sampleLoad("load-alive")
        loadRepo.insertLoad(load, playFeedback = false)
        val orphanFile = File(scansDir, "stale.pdf").apply { writeText("%PDF-stale") }
        db.scanDao().insert(
            ScanEntity(
                id = "scan-orphan-load",
                fileName = orphanFile.name,
                filePath = orphanFile.absolutePath,
                timestamp = 1L,
                fileSizeBytes = 9,
                pageCount = 1,
                ocrText = "",
                loadId = "load-gone",
            ),
        )

        val removed = loadRepo.cleanupOrphanAttachments()

        assertEquals(1, removed)
        assertTrue(db.scanDao().getAllScansOnce().isEmpty())
        assertFalse(orphanFile.exists())
        assertEquals(1, loadRepo.getAllLoadsOnce().size)
    }

    private fun sampleLoad(id: String) = Load(
        id = id,
        tripId = "T-$id",
        date = "2026-07-21",
        totalRate = 1000.0,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 30,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
    )
}
