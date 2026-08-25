package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.LoadHistory
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LoadUpdatePersistsFieldsTest {

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
    fun updateLoad_persistsTripIdCountsAndRawMessage() = runBlocking {
        repo.insertLoad(sampleLoad("load-1", tripId = "T-OLD"), playFeedback = false)

        repo.updateLoad(
            sampleLoad("load-1", tripId = "t-new").copy(
                puCount = 2,
                delCount = 3,
                rawMessage = "updated raw",
                stops = listOf(
                    Stop(0, "load-1", 1, StopType.PU, null, null, "07/21 08:00 EDT", "EDT", null, "Austin, TX", "Austin", "TX", ""),
                    Stop(0, "load-1", 2, StopType.DEL, null, null, "07/21 18:00 EDT", "EDT", null, "Dallas, TX", "Dallas", "TX", ""),
                    Stop(0, "load-1", 3, StopType.DEL, null, null, "07/22 09:00 EDT", "EDT", null, "Houston, TX", "Houston", "TX", ""),
                ),
            ),
        )

        val reloaded = repo.getLoadById("load-1")
        assertNotNull(reloaded)
        assertEquals("T-NEW", reloaded!!.tripId)
        assertEquals(1, reloaded.puCount)
        assertEquals(2, reloaded.delCount)
        assertEquals("updated raw", reloaded.rawMessage)
        assertEquals("T-NEW", repo.getByTripId("t-new")?.tripId)
    }

    @Test
    fun insertLoad_normalizesTripIdForLookup() = runBlocking {
        repo.insertLoad(sampleLoad("load-case", tripId = "t-abc"), playFeedback = false)
        val found = repo.getByTripId("T-ABC")
        assertNotNull(found)
        assertEquals("T-ABC", found!!.tripId)
    }

    @Test
    fun deleteAllLoads_clearsHistory() = runBlocking {
        repo.insertLoad(sampleLoad("load-h", tripId = "T-H"), playFeedback = false)
        db.loadHistoryDao().insert(
            LoadHistory(
                id = "hist-1",
                loadId = "load-h",
                field = "totalRate",
                oldValue = "1",
                newValue = "2",
                timestamp = 1L,
            ),
        )
        assertEquals(1, db.loadHistoryDao().getHistory("load-h").size)
        repo.deleteAllLoads()
        assertTrue(repo.getAllLoadsOnce().isEmpty())
        assertTrue(db.loadHistoryDao().getHistory("load-h").isEmpty())
    }

    @Test
    fun updateLoad_persistsDisputePayoutFields() = runBlocking {
        repo.insertLoad(sampleLoad("load-d", tripId = "T-DSP"), playFeedback = false)
        repo.updateLoad(
            sampleLoad("load-d", tripId = "T-DSP").copy(
                totalRate = 2250.0,
                isDispute = true,
                disputeResponseDate = "2026-08-27",
                disputeCompleted = true,
                disputeAmount = 250.0,
                disputeApplyToLoad = true,
                disputeAmountApplied = true,
            ),
        )
        val reloaded = repo.getLoadById("load-d")!!
        assertEquals(2250.0, reloaded.totalRate, 0.0)
        assertEquals(250.0, reloaded.disputeAmount)
        assertTrue(reloaded.disputeApplyToLoad)
        assertTrue(reloaded.disputeAmountApplied)
        assertTrue(reloaded.hadDispute)
    }

    private fun sampleLoad(id: String, tripId: String) = Load(
        id = id,
        tripId = tripId,
        date = "2026-07-21",
        totalRate = 1000.0,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 30,
        year = 2026,
        rawMessage = "old raw",
        parsedAt = 1L,
        updatedAt = 1L,
    )
}
