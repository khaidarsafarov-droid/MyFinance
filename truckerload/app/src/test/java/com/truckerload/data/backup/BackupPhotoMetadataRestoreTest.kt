package com.truckerload.data.backup

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.repository.LoadRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Photo GPS, date and time are an intended feature and must survive backup/restore.
 * Journal JSON does not carry the JPEG bytes; the Room row and the on-disk file stay
 * on the device, and restore must not alter that metadata.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupPhotoMetadataRestoreTest {

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
        mediaDir = File(context.cacheDir, "photo_meta_restore").apply {
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
    fun restore_keepsPhotoGpsDateAndTime() = runBlocking {
        val load = BackupTestFixtures.sampleLoad(id = "load-kept")
        loadRepo.insertLoad(load, playFeedback = false)

        val photoFile = File(mediaDir, "garner.jpg")
        val originalBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x45, 0x78, 0x69, 0x66)
        photoFile.writeBytes(originalBytes)

        val original = PhotoEntity(
            id = "photo-gps",
            fileName = photoFile.name,
            filePath = photoFile.absolutePath,
            latitude = 35.711001,
            longitude = -78.614002,
            city = "Garner",
            state = "NC",
            zipCode = "27529",
            timestamp = 1_720_000_000_000L,
            loadId = load.id,
        )
        db.photoDao().insert(original)

        BackupRoomApplier.applyFullReplace(
            db,
            BackupData(
                schemaVersion = BackupSchema.CURRENT,
                accountId = "user-abc",
                loads = listOf(load),
            ),
        )
        BackupRoomApplier.pruneOrphanMedia(db)

        val restored = db.photoDao().getAllPhotosOnce().single()
        assertEquals(original.id, restored.id)
        assertEquals(35.711001, restored.latitude, 0.0)
        assertEquals(-78.614002, restored.longitude, 0.0)
        assertEquals(1_720_000_000_000L, restored.timestamp)
        assertEquals("Garner", restored.city)
        assertEquals("NC", restored.state)
        assertEquals("27529", restored.zipCode)
        assertEquals(photoFile.absolutePath, restored.filePath)
        assertTrue(photoFile.exists())
        assertArrayEquals(originalBytes, photoFile.readBytes())
    }
}
