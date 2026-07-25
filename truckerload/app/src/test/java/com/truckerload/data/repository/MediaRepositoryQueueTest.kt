package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.data.sync.MediaSyncEnqueuer
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
    private val queue = RecordingMediaQueue()

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
    fun `photo save queues upsert and delete supersedes local row immediately`() = runBlocking {
        val file = File.createTempFile("queued-photo", ".jpg")
        val repository = PhotoRepository(db, queue)
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
            assertEquals(PhotoEntity.CLOUD_PENDING, db.photoDao().getById(photo.id)?.cloudSyncStatus)
            assertEquals(listOf("UPSERT:${photo.id}"), queue.events)

            repository.deletePhoto(photo.id)
            assertNull(db.photoDao().getById(photo.id))
            assertEquals(listOf("UPSERT:${photo.id}", "DELETE:${photo.id}"), queue.events)
            assertEquals(2, queue.schedules)
        } finally {
            file.delete()
        }
    }
}

private class RecordingMediaQueue : MediaSyncEnqueuer {
    val events = mutableListOf<String>()
    var schedules = 0

    override fun enabled() = true
    override suspend fun enqueuePhotoUpsert(photo: PhotoEntity) {
        events += "UPSERT:${photo.id}"
    }
    override suspend fun enqueueScanUpsert(scan: ScanEntity) {
        events += "UPSERT:${scan.id}"
    }
    override suspend fun enqueuePhotoDelete(photo: PhotoEntity) {
        events += "DELETE:${photo.id}"
    }
    override suspend fun enqueueScanDelete(scan: ScanEntity) {
        events += "DELETE:${scan.id}"
    }
    override fun schedule() {
        schedules++
    }
}
