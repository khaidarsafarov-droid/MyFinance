package com.truckerload.data.backup

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.data.repository.LoadRepository
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
class BackupScopedRestoreTest {

    private lateinit var db: AppDatabase
    private lateinit var loadRepo: LoadRepository
    private lateinit var mediaDir: File

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        loadRepo = LoadRepository(db)
        mediaDir = File(context.cacheDir, "scoped_restore_media").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        db.close()
        mediaDir.deleteRecursively()
    }

    @Test
    fun scopedRestore_keepsMediaForRestoredLoadsAndUnlinked() = runBlocking {
        val keptLoad = BackupTestFixtures.sampleLoad(id = "load-kept")
        val removedLoad = sampleLoad(id = "load-removed", tripId = "T-REMOVED")
        loadRepo.insertLoad(keptLoad, playFeedback = false)
        loadRepo.insertLoad(removedLoad, playFeedback = false)

        val keptPhotoFile = mediaFile("kept.jpg")
        val removedPhotoFile = mediaFile("removed.jpg")
        val unlinkedPhotoFile = mediaFile("unlinked.jpg")
        insertPhoto("photo-kept", keptPhotoFile, keptLoad.id)
        insertPhoto("photo-removed", removedPhotoFile, removedLoad.id)
        insertPhoto("photo-unlinked", unlinkedPhotoFile, loadId = null)

        val backup = BackupData(
            accountId = "user-abc",
            loads = listOf(keptLoad),
            paychecks = emptyList(),
            diesel = emptyList(),
        )
        BackupRoomApplier.applyFullReplace(db, backup)
        BackupRoomApplier.pruneOrphanMedia(db)

        val photoIds = db.photoDao().getAllPhotosOnce().map { it.id }.toSet()
        assertEquals(setOf("photo-kept", "photo-unlinked"), photoIds)
        assertTrue(keptPhotoFile.exists())
        assertTrue(unlinkedPhotoFile.exists())
        assertFalse(removedPhotoFile.exists())
        assertEquals(1, loadRepo.getAllLoadsOnce().size)
    }

    @Test
    fun scopedRestore_prunesScansForRemovedLoads() = runBlocking {
        val keptLoad = BackupTestFixtures.sampleLoad(id = "load-a")
        val staleLoad = sampleLoad(id = "load-b", tripId = "T-B")
        loadRepo.insertLoad(keptLoad, playFeedback = false)
        loadRepo.insertLoad(staleLoad, playFeedback = false)

        val keptScanFile = mediaFile("kept.pdf")
        val staleScanFile = mediaFile("stale.pdf")
        db.scanDao().insert(
            ScanEntity(
                id = "scan-kept",
                fileName = keptScanFile.name,
                filePath = keptScanFile.absolutePath,
                timestamp = 1L,
                fileSizeBytes = 8,
                pageCount = 1,
                ocrText = "",
                loadId = keptLoad.id,
            ),
        )
        db.scanDao().insert(
            ScanEntity(
                id = "scan-stale",
                fileName = staleScanFile.name,
                filePath = staleScanFile.absolutePath,
                timestamp = 2L,
                fileSizeBytes = 8,
                pageCount = 1,
                ocrText = "",
                loadId = staleLoad.id,
            ),
        )

        BackupRoomApplier.applyFullReplace(
            db,
            BackupData(loads = listOf(keptLoad)),
        )
        BackupRoomApplier.pruneOrphanMedia(db)

        assertEquals(listOf("scan-kept"), db.scanDao().getAllScansOnce().map { it.id })
        assertTrue(keptScanFile.exists())
        assertFalse(staleScanFile.exists())
    }

    private suspend fun insertPhoto(id: String, file: File, loadId: String?) {
        db.photoDao().insert(
            PhotoEntity(
                id = id,
                fileName = file.name,
                filePath = file.absolutePath,
                latitude = 0.0,
                longitude = 0.0,
                city = "",
                state = "",
                zipCode = "",
                timestamp = 1L,
                loadId = loadId,
            ),
        )
    }

    private fun mediaFile(name: String): File =
        File(mediaDir, name).apply { writeText("media-$name") }

    private fun sampleLoad(id: String, tripId: String) = Load(
        id = id,
        tripId = tripId,
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
