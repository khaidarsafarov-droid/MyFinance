package com.truckerload.utils

import androidx.room.Room
import com.truckerload.data.backup.BackupData
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.google.gson.Gson
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupRestoreRoundTripTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: LoadRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        // Seed AppDatabase application context for backup helpers if needed.
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
    fun backupJson_roundTripsLoadCount() = runBlocking {
        val load = Load(
            id = "L1",
            tripId = "T-BACKUP1",
            date = "2026-07-21",
            totalRate = 2500.0,
            totalMiles = 850.0,
            pointA = "Garner, NC",
            pointB = "Dallas, TX",
            puCount = 1,
            delCount = 1,
            weekNumber = 30,
            year = 2026,
            rawMessage = "Trip ID: T-BACKUP1",
            parsedAt = 1L,
            updatedAt = 1L,
        )
        repo.insertLoad(load, playFeedback = false)
        val all = repo.getAllLoadsOnce()
        assertEquals(1, all.size)

        val json = Gson().toJson(BackupData(loads = all, paychecks = emptyList(), diesel = emptyList()))
        assertTrue(json.contains("T-BACKUP1"))

        // Wipe and re-insert via same shape BackupService uses.
        db.loadDao().deleteAll()
        assertTrue(repo.getAllLoadsOnce().isEmpty())

        val restored = Gson().fromJson(json, BackupData::class.java)
        restored.loads.forEach { repo.insertLoad(it, playFeedback = false) }
        assertEquals(1, repo.getAllLoadsOnce().size)
        assertEquals("T-BACKUP1", repo.getAllLoadsOnce().first().tripId)
    }
}
