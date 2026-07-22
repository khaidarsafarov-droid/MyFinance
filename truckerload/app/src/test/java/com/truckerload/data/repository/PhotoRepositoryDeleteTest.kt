package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
class PhotoRepositoryDeleteTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PhotoRepository
    private lateinit var photoFile: File

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PhotoRepository(db)
        photoFile = File(context.cacheDir, "photo_delete_${System.currentTimeMillis()}.jpg").apply {
            writeText("fake-jpeg")
        }
    }

    @After
    fun tearDown() {
        db.close()
        photoFile.delete()
    }

    @Test
    fun deletePhoto_removesDbRowAndFile() = runBlocking {
        db.photoDao().insert(
            PhotoEntity(
                id = "photo-del-1",
                fileName = photoFile.name,
                filePath = photoFile.absolutePath,
                latitude = 0.0,
                longitude = 0.0,
                city = "",
                state = "",
                zipCode = "",
                timestamp = 1L,
                loadId = null,
            ),
        )
        assertTrue(photoFile.exists())

        repo.deletePhoto("photo-del-1")

        assertNull(db.photoDao().getById("photo-del-1"))
        assertFalse(photoFile.exists())
    }

    @Test
    fun deletePhoto_missingFileStillDeletesRow() = runBlocking {
        db.photoDao().insert(
            PhotoEntity(
                id = "photo-del-2",
                fileName = "gone.jpg",
                filePath = File(RuntimeEnvironment.getApplication().cacheDir, "does_not_exist.jpg").absolutePath,
                latitude = 0.0,
                longitude = 0.0,
                city = "",
                state = "",
                zipCode = "",
                timestamp = 1L,
                loadId = null,
            ),
        )

        repo.deletePhoto("photo-del-2")

        assertNull(db.photoDao().getById("photo-del-2"))
    }
}
