package com.truckerload.data.local

import androidx.room.Room
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.utils.VerifiedFileCopy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
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

/** Photos, scans and local backups move from external to internal storage without loss. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaStorageMigrationTest {

    private lateinit var db: AppDatabase
    private lateinit var legacy: File
    private lateinit var internal: File

    private val photoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + "Exif GPS 35.7 -78.6".toByteArray()
    private val scanBytes = "%PDF-1.7 bol".toByteArray()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        legacy = File(context.cacheDir, "mig/external").apply {
            deleteRecursively()
            mkdirs()
        }
        internal = File(context.cacheDir, "mig/internal").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        db.close()
        legacy.parentFile?.deleteRecursively()
    }

    private fun seed(): Triple<File, File, File> {
        val photo = File(legacy, "photos/T1_2026-07-02.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(photoBytes)
        }
        val scan = File(legacy, "scans/scan_1.pdf").apply {
            parentFile?.mkdirs()
            writeBytes(scanBytes)
        }
        val auto = File(legacy, "backups/user-abc/auto/auto_backup_20260702_101112.tlb").apply {
            parentFile?.mkdirs()
            writeText("{\"schemaVersion\":2}")
        }
        runBlocking {
            db.photoDao().insert(photoRow(photo))
            db.scanDao().insert(scanRow(scan))
        }
        return Triple(photo, scan, auto)
    }

    private fun photoRow(file: File) = PhotoEntity(
        id = "photo-1",
        fileName = file.name,
        filePath = file.absolutePath,
        latitude = 35.7,
        longitude = -78.6,
        city = "Garner",
        state = "NC",
        zipCode = "27529",
        timestamp = 1_720_000_000_000L,
        loadId = null,
    )

    private fun scanRow(file: File) = ScanEntity(
        id = "scan-1",
        fileName = file.name,
        filePath = file.absolutePath,
        timestamp = 1L,
        fileSizeBytes = file.length(),
        pageCount = 1,
        ocrText = "",
        loadId = null,
    )

    private fun sqlite() = db.openHelper.writableDatabase

    @Test
    fun movesFilesUpdatesRowsThenDeletesOldCopies() = runBlocking {
        val (photo, scan, auto) = seed()
        val manualFallback = File(legacy, "backups/TruckoRig_Backup_1.json").apply { writeText("{}") }

        val report = MediaStorageMigration.run(legacy, internal, sqlite())

        assertEquals(3, report.copied)
        assertEquals(3, report.deleted)
        val movedPhoto = File(internal, "photos/T1_2026-07-02.jpg")
        assertArrayEquals(photoBytes, movedPhoto.readBytes())
        assertArrayEquals(scanBytes, File(internal, "scans/scan_1.pdf").readBytes())
        assertTrue(File(internal, "backups/user-abc/auto/auto_backup_20260702_101112.tlb").isFile)
        val row = db.photoDao().getById("photo-1")!!
        assertEquals(movedPhoto.absolutePath, row.filePath)
        assertEquals(35.7, row.latitude, 0.0)
        assertEquals(File(internal, "scans/scan_1.pdf").absolutePath, db.scanDao().getById("scan-1")!!.filePath)
        assertFalse(photo.exists())
        assertFalse(scan.exists())
        assertFalse(auto.exists())
        // A user-visible export fallback file is not part of the move.
        assertTrue(manualFallback.exists())
        assertEquals(0, MediaStorageMigration.pendingFiles(legacy))
    }

    @Test
    fun secondRunChangesNothing() = runBlocking {
        seed()
        MediaStorageMigration.run(legacy, internal, sqlite())
        val again = MediaStorageMigration.run(legacy, internal, sqlite())
        assertEquals(MediaStorageMigration.Report(0, 0, 0, 0), again)
    }

    @Test
    fun crashAfterCopyBeforeDeleteFinishesOnNextRun() = runBlocking {
        val (photo, _, _) = seed()
        // Previous run copied the photo and died before the row update and delete.
        File(internal, "photos/T1_2026-07-02.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(photoBytes)
        }
        // And left a torn temp file from the scan copy.
        File(internal, "scans/scan_1.pdf${VerifiedFileCopy.TEMP_SUFFIX}").apply {
            parentFile?.mkdirs()
            writeBytes(scanBytes.copyOf(3))
        }

        val report = MediaStorageMigration.run(legacy, internal, sqlite())

        assertEquals(1, report.alreadyThere)
        assertEquals(2, report.copied)
        assertFalse(photo.exists())
        assertArrayEquals(scanBytes, File(internal, "scans/scan_1.pdf").readBytes())
        assertFalse(File(internal, "scans/scan_1.pdf${VerifiedFileCopy.TEMP_SUFFIX}").exists())
    }

    @Test
    fun differentFileUnderTheSameNameIsNeverOverwrittenAndTheOldOneStays() = runBlocking {
        val (photo, _, _) = seed()
        val clash = File(internal, "photos/T1_2026-07-02.jpg").apply {
            parentFile?.mkdirs()
            writeBytes("another photo".toByteArray())
        }

        val report = MediaStorageMigration.run(legacy, internal, sqlite())

        assertEquals(1, report.kept)
        assertTrue(photo.exists())
        assertArrayEquals("another photo".toByteArray(), clash.readBytes())
        // The row keeps pointing at the original, which still exists.
        assertEquals(photo.absolutePath, db.photoDao().getById("photo-1")!!.filePath)
    }

    @Test
    fun anotherAccountsJournalIsRewrittenWhenItOpens() = runBlocking {
        val (photo, _, _) = seed()
        // Files move while this journal is closed (no db passed); its rows still name the old path.
        MediaStorageMigration.run(legacy, internal, null)
        assertFalse(photo.exists())
        assertEquals(photo.absolutePath, db.photoDao().getById("photo-1")!!.filePath)

        MediaStorageMigration.rewritePaths(sqlite(), legacy, internal)

        assertEquals(File(internal, "photos/T1_2026-07-02.jpg").absolutePath, db.photoDao().getById("photo-1")!!.filePath)
    }

    @Test
    fun rowIsNotRewrittenToACopyThatDiffers() = runBlocking {
        seed()
        File(internal, "photos/T1_2026-07-02.jpg").apply {
            parentFile?.mkdirs()
            writeBytes("different".toByteArray())
        }
        val updated = MediaStorageMigration.rewritePaths(sqlite(), legacy, internal)
        assertEquals(0, updated)
        assertEquals(0, db.photoDao().getAllPhotosOnce().count { it.filePath.startsWith(internal.absolutePath) })
    }
}
