package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.DeletedLoadLedger
import com.truckerload.domain.model.Load
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReviveDeletedLoadTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: LoadRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        AppDatabase.getInstance(context, "revive-deleted-test")
        AppDatabase.closeCurrent()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LoadRepository(db)
        context.getSharedPreferences("truckerload_deleted_loads", 0).edit().clear().commit()
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.closeCurrent()
    }

    @Test
    fun explicitInsert_revivesLoadTheBotBlockedAfterDelete() = runBlocking {
        val load = sample("T-REVIVE")
        repo.insertLoad(load, playFeedback = false)
        repo.deleteLoad(load.id)
        assertNull(repo.getByTripId("T-REVIVE"))

        repo.insertLoad(load, playFeedback = false, reviveDeleted = false)
        assertNull(repo.getByTripId("T-REVIVE"))

        repo.insertLoad(load, playFeedback = false, reviveDeleted = true)
        assertNotNull(repo.getByTripId("T-REVIVE"))
        assertFalse(
            DeletedLoadLedger.isBlocked(
                RuntimeEnvironment.getApplication(),
                load.id,
                load.tripId,
            ),
        )
    }

    private fun sample(tripId: String) = Load(
        id = tripId,
        tripId = tripId,
        date = "2026-09-01",
        totalRate = 2500.0,
        totalMiles = 850.0,
        pointA = "Garner, NC",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = 36,
        year = 2026,
        rawMessage = "Trip ID: $tripId",
        parsedAt = 1L,
        updatedAt = 1L,
    )
}
