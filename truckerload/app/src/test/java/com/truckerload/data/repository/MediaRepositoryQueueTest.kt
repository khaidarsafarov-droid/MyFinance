package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaRepositoryQueueTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `photo save stays local and delete removes the row`() = runBlocking {
        val file = File.createTempFile("queued-photo", ".jpg")
        val repository = PhotoRepository(db)
        try {
            val photo = repository.savePhoto(
                file.name,
                file.absolutePath,
                1.0,
                2.0,
                "Raleigh",
                "NC",
                "27601",
                10,
            )
            assertEquals(PhotoEntity.CLOUD_LOCAL, db.photoDao().getById(photo.id)?.cloudSyncStatus)

            repository.deletePhoto(photo.id)
            assertNull(db.photoDao().getById(photo.id))
        } finally {
            file.delete()
        }
    }
}
