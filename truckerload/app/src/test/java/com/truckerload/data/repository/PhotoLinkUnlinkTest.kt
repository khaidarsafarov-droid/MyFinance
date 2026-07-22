package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
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
class PhotoLinkUnlinkTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PhotoRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PhotoRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun linkPhotoToLoad_thenUnlink_updatesLoadId() = runBlocking {
        db.photoDao().insert(
            PhotoEntity(
                id = "photo-link-1",
                fileName = "a.jpg",
                filePath = "/tmp/a.jpg",
                latitude = 0.0,
                longitude = 0.0,
                city = "",
                state = "",
                zipCode = "",
                timestamp = 1L,
                loadId = null,
            ),
        )

        repo.linkPhotoToLoad("photo-link-1", "load-42")
        assertEquals("load-42", db.photoDao().getById("photo-link-1")?.loadId)
        assertEquals(1, db.photoDao().getPhotosByLoadIdOnce("load-42").size)

        repo.linkPhotoToLoad("photo-link-1", null)
        assertNull(db.photoDao().getById("photo-link-1")?.loadId)
        assertEquals(0, db.photoDao().getPhotosByLoadIdOnce("load-42").size)
    }
}
