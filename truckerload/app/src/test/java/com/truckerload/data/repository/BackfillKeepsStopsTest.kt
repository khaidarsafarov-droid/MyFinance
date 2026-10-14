package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.truckerload.domain.money.Money

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackfillKeepsStopsTest {

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
    fun backfillPuDelMillis_keepsStopsAndPenalties() = runBlocking {
        insertLoadWithMissingMillis()

        repo.backfillPuDelMillisFromStops()

        assertEquals(2, db.stopDao().getStopsByLoadId(LOAD_ID).size)
        assertEquals(1, db.penaltyDao().getPenaltiesByLoadId(LOAD_ID).size)
    }

    @Test
    fun backfillRouteMetrics_keepsStopsAndPenalties() = runBlocking {
        insertLoadWithMissingMillis()

        repo.backfillRouteMetricsFromStops()

        assertEquals(2, db.stopDao().getStopsByLoadId(LOAD_ID).size)
        assertEquals(1, db.penaltyDao().getPenaltiesByLoadId(LOAD_ID).size)
    }

    private suspend fun insertLoadWithMissingMillis() {
        repo.insertLoad(sampleLoad(), playFeedback = false)
        db.penaltyDao().insertAll(
            listOf(PenaltyEntity(loadId = LOAD_ID, description = "Late", amount = Money.fromDollars(50.0).cents)),
        )
        val entity = db.loadDao().getLoadById(LOAD_ID)!!
        db.loadDao().updateEntity(entity.copy(firstPuMillis = null, lastDelMillis = null, route = ""))
    }

    private fun sampleLoad() = Load(
        id = LOAD_ID,
        tripId = "T-BACKFILL",
        date = "2026-07-21",
        totalRate = Money.fromDollars(1000.0),
        totalMiles = 300.0,
        pointA = "Austin, TX",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = 30,
        year = 2026,
        rawMessage = "raw",
        parsedAt = 1L,
        updatedAt = 1L,
        stops = listOf(
            Stop(0, LOAD_ID, 1, StopType.PU, null, null, "07/21 08:00 EDT", "EDT", null, "Austin, TX", "Austin", "TX", ""),
            Stop(0, LOAD_ID, 2, StopType.DEL, null, null, "07/21 18:00 EDT", "EDT", null, "Dallas, TX", "Dallas", "TX", ""),
        ),
    )

    private companion object {
        const val LOAD_ID = "load-backfill"
    }
}
