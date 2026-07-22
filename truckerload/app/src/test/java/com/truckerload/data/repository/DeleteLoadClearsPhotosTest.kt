package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.domain.model.Load
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class DeleteLoadClearsPhotosTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: LoadRepository
    private lateinit var photoFile: File

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LoadRepository(db)
        photoFile = File(context.cacheDir, "test_photo_${System.currentTimeMillis()}.jpg").apply {
            writeText("fake-jpeg")
        }
    }

    @After
    fun tearDown() {
        db.close()
        photoFile.delete()
    }

    @Test
    fun deleteLoad_removesPhotoRowsAndFiles() = runBlocking {
        val load = Load(
            id = "load-1",
            tripId = "T-TEST1",
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
        repo.insertLoad(load, playFeedback = false)
        db.photoDao().insert(
            PhotoEntity(
                id = "photo-1",
                fileName = photoFile.name,
                filePath = photoFile.absolutePath,
                latitude = 0.0,
                longitude = 0.0,
                city = "",
                state = "",
                zipCode = "",
                timestamp = 1L,
                loadId = load.id,
            ),
        )
        assertEquals(1, db.photoDao().getPhotosByLoadIdOnce(load.id).size)
        assertTrue(photoFile.exists())

        repo.deleteLoad(load.id)

        assertTrue(db.photoDao().getPhotosByLoadIdOnce(load.id).isEmpty())
        assertTrue(db.loadDao().getLoadById(load.id) == null)
        assertTrue(!photoFile.exists())
    }

    @Test
    fun deleteLoad_removesScanRowsAndFiles() = runBlocking {
        val load = Load(
            id = "load-2",
            tripId = "T-TEST2",
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
        repo.insertLoad(load, playFeedback = false)
        val scanFile = File(RuntimeEnvironment.getApplication().cacheDir, "test_scan.pdf").apply {
            writeText("%PDF-fake")
        }
        db.scanDao().insert(
            com.truckerload.data.local.entities.ScanEntity(
                id = "scan-1",
                fileName = scanFile.name,
                filePath = scanFile.absolutePath,
                timestamp = 1L,
                fileSizeBytes = 9,
                pageCount = 1,
                ocrText = "",
                loadId = load.id,
            ),
        )
        assertEquals(1, db.scanDao().getScansByLoadIdOnce(load.id).size)
        repo.deleteLoad(load.id)
        assertTrue(db.scanDao().getScansByLoadIdOnce(load.id).isEmpty())
        assertTrue(!scanFile.exists())
    }
}
