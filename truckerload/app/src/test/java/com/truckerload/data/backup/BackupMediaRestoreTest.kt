package com.truckerload.data.backup

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * A zip backup restored on a new phone brings photos and scans back: same bytes (EXIF GPS,
 * date, time), the rows' GPS/date/time, and paths that point at the new files.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupMediaRestoreTest {

    private lateinit var db: AppDatabase
    private lateinit var root: File
    private lateinit var photosDir: File
    private lateinit var scansDir: File

    private val photoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte()) +
        "Exif GPS 35.711001 -78.614002 2026:07:02 10:11:12".toByteArray()
    private val scanBytes = "%PDF-1.7 BOL".toByteArray()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        root = File(context.cacheDir, "media-restore-test").apply {
            deleteRecursively()
            mkdirs()
        }
        photosDir = File(root, "new-phone/photos")
        scansDir = File(root, "new-phone/scans")
    }

    @After
    fun tearDown() {
        db.close()
        root.deleteRecursively()
    }

    private fun oldPhoneArchive(): Pair<BackupData, ByteArray> {
        val oldDir = File(root, "old-phone").apply { mkdirs() }
        val photo = File(oldDir, "garner.jpg").apply { writeBytes(photoBytes) }
        val scan = File(oldDir, "bol.pdf").apply { writeBytes(scanBytes) }
        val backup = BackupData(
            schemaVersion = BackupSchema.CURRENT,
            accountId = "user-abc",
            loads = listOf(BackupTestFixtures.sampleLoad(id = "load-1")),
            photos = listOf(
                BackupPhoto(
                    id = "photo-1",
                    fileName = photo.name,
                    filePath = photo.absolutePath,
                    latitude = 35.711001,
                    longitude = -78.614002,
                    city = "Garner",
                    state = "NC",
                    zipCode = "27529",
                    timestamp = 1_720_000_000_000L,
                    loadId = "load-1",
                ),
            ),
            scans = listOf(
                BackupScan(id = "scan-1", fileName = scan.name, filePath = scan.absolutePath, loadId = "load-1"),
            ),
        )
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, BackupDataCodec.toUtf8Bytes(backup), BackupArchive.mediaEntries(backup))
        oldDir.deleteRecursively()
        return backup to out.toByteArray()
    }

    private fun readArchive(bytes: ByteArray): Pair<BackupArchive.Contents, BackupData> {
        val contents = BackupArchive.read(ByteArrayInputStream(bytes), File(root, "staging"), 1 shl 20)
        return contents to BackupDataCodec.decode(contents.json)
    }

    @Test
    fun newPhoneGetsPhotosAndScansBack() = runBlocking {
        val (_, archive) = oldPhoneArchive()
        val (contents, decoded) = readArchive(archive)

        val placement = BackupMediaPlacer.place(contents, decoded, BackupMediaPlacer.Targets(photosDir, scansDir))
        BackupRoomApplier.applyFullReplace(db, placement.backup)

        val photo = db.photoDao().getAllPhotosOnce().single()
        assertEquals(File(photosDir, "garner.jpg").absolutePath, photo.filePath)
        assertArrayEquals(photoBytes, File(photo.filePath).readBytes())
        assertEquals(35.711001, photo.latitude, 0.0)
        assertEquals(-78.614002, photo.longitude, 0.0)
        assertEquals(1_720_000_000_000L, photo.timestamp)
        assertEquals("load-1", photo.loadId)
        val scan = db.scanDao().getAllScansOnce().single()
        assertArrayEquals(scanBytes, File(scan.filePath).readBytes())
        assertEquals(2, placement.created.size)
    }

    @Test
    fun sameFileAlreadyHereIsReusedAndADifferentOneIsNeverOverwritten() = runBlocking {
        val (_, archive) = oldPhoneArchive()
        photosDir.mkdirs()
        scansDir.mkdirs()
        File(photosDir, "garner.jpg").writeBytes(photoBytes)
        val other = File(scansDir, "bol.pdf").apply { writeBytes("%PDF other document".toByteArray()) }
        val (contents, decoded) = readArchive(archive)

        val placement = BackupMediaPlacer.place(contents, decoded, BackupMediaPlacer.Targets(photosDir, scansDir))

        assertEquals(File(photosDir, "garner.jpg").absolutePath, placement.backup.photos!!.single().filePath)
        assertEquals(File(scansDir, "bol_1.pdf").absolutePath, placement.backup.scans!!.single().filePath)
        assertArrayEquals("%PDF other document".toByteArray(), other.readBytes())
        assertEquals(listOf(File(scansDir, "bol_1.pdf")), placement.created)
    }

    @Test
    fun rowWithItsFileStaysAndARowWithAMissingFileIsRepaired() = runBlocking {
        val (_, archive) = oldPhoneArchive()
        val local = File(root, "local.jpg").apply { writeBytes(photoBytes) }
        db.photoDao().insert(
            PhotoEntity(
                id = "photo-1",
                fileName = local.name,
                filePath = local.absolutePath,
                latitude = 1.0,
                longitude = 2.0,
                city = "",
                state = "",
                zipCode = "",
                timestamp = 5L,
                loadId = null,
            ),
        )
        db.scanDao().insert(
            ScanEntity(
                id = "scan-1",
                fileName = "lost.pdf",
                filePath = File(root, "lost.pdf").absolutePath,
                timestamp = 1L,
                fileSizeBytes = 1,
                pageCount = 1,
                ocrText = "",
                loadId = null,
            ),
        )
        val (contents, decoded) = readArchive(archive)

        val placement = BackupMediaPlacer.place(
            contents,
            decoded,
            BackupMediaPlacer.Targets(photosDir, scansDir, presentPhotoIds = setOf("photo-1")),
        )
        BackupRoomApplier.applyFullReplace(db, placement.backup)

        assertEquals(local.absolutePath, db.photoDao().getById("photo-1")!!.filePath)
        assertFalse(File(photosDir, "garner.jpg").exists())
        val scan = db.scanDao().getById("scan-1")!!
        assertEquals(File(scansDir, "bol.pdf").absolutePath, scan.filePath)
        assertArrayEquals(scanBytes, File(scan.filePath).readBytes())
    }

    @Test
    fun failedJournalWriteRemovesOnlyFilesThisRestoreCreated() {
        val (_, archive) = oldPhoneArchive()
        photosDir.mkdirs()
        val existing = File(photosDir, "garner.jpg").apply { writeBytes(photoBytes) }
        val (contents, decoded) = readArchive(archive)
        val placement = BackupMediaPlacer.place(contents, decoded, BackupMediaPlacer.Targets(photosDir, scansDir))

        BackupMediaPlacer.discard(placement.created)

        assertTrue(existing.exists())
        assertFalse(File(scansDir, "bol.pdf").exists())
    }

    @Test
    fun plainJsonBackupStillRestoresAndLeavesFilesAlone() = runBlocking {
        val local = File(root, "kept.jpg").apply { writeBytes(photoBytes) }
        val (backup, _) = oldPhoneArchive()
        db.photoDao().insert(BackupPhoto.from(backup.photos!!.single().toEntity()).copy(filePath = local.absolutePath).toEntity())

        val json = BackupDataCodec.decode(BackupDataCodec.toJson(backup))
        BackupRoomApplier.applyFullReplace(db, json)

        assertEquals(local.absolutePath, db.photoDao().getById("photo-1")!!.filePath)
        assertTrue(local.exists())
    }
}
