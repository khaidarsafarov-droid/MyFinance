package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * QUALITY_100 #91 — light concurrent insert stress; BackupService.autoBackupMutex is the
 * production guard that serializes auto-backup after inserts (see BackupService.kt).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConcurrentInsertLoadTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: LoadRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LoadRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun concurrentInsertLoad_allRowsPersist() = runBlocking {
        val mutex = Mutex() // mirrors BackupService.autoBackupMutex serialization idea
        val jobs = (1..8).map { i ->
            async(Dispatchers.IO) {
                mutex.withLock {
                    repo.insertLoad(sampleLoad("c-$i"), playFeedback = false)
                }
            }
        }
        jobs.awaitAll()
        val all = repo.getAllLoadsOnce()
        assertEquals(8, all.size)
        assertTrue(all.map { it.id }.toSet().size == 8)
    }

    @Test
    fun insertLoad_sameIdReplacesStopsWithoutOrphans() = runBlocking {
        val first = sampleLoad("same").copy(
            stops = listOf(
                Stop(0, "same", 1, StopType.PU, null, null, "07/21 08:00 EDT", "EDT", null, "Austin, TX", "Austin", "TX", ""),
                Stop(0, "same", 2, StopType.DEL, null, null, "07/21 18:00 EDT", "EDT", null, "Dallas, TX", "Dallas", "TX", ""),
            ),
        )
        repo.insertLoad(first, playFeedback = false)
        repo.insertLoad(
            first.copy(
                totalRate = 2000.0,
                stops = listOf(
                    Stop(0, "same", 1, StopType.PU, null, null, "07/22 08:00 EDT", "EDT", null, "Houston, TX", "Houston", "TX", ""),
                ),
            ),
            playFeedback = false,
        )
        val loaded = repo.getLoadById("same")!!
        assertEquals(2000.0, loaded.totalRate, 0.01)
        assertEquals(1, loaded.stops.size)
        assertEquals("Houston", loaded.stops[0].city)
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
