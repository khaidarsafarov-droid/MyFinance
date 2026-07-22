package com.truckerload.data.repository

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.normalizeTripId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import androidx.room.Room
import com.truckerload.data.local.AppDatabase

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SyncLoadsCdcTest {

    private fun repo(): LoadRepository {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return LoadRepository(db)
    }

    private fun sample(tripId: String, rate: Double = 1000.0) = Load(
        id = "id-$tripId",
        tripId = tripId,
        date = "2026-07-21",
        totalRate = rate,
        totalMiles = 100.0,
        pointA = "A, NC",
        pointB = "B, FL",
        puCount = 1,
        delCount = 1,
        weekNumber = 30,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun cdc_insertsNewLoad() = runBlocking {
        val repo = repo()
        val result = repo.syncLoadsCdc(listOf(sample("t-abc")), messageDateSeconds = null, playFeedback = false)
        assertEquals(SyncStatus.SUCCESS, result.status)
        assertEquals(1, result.addedCount)
    }

    @Test
    fun cdc_duplicateCaseInsensitive() = runBlocking {
        val repo = repo()
        repo.syncLoadsCdc(listOf(sample("T-ABC")), null, playFeedback = false)
        val result = repo.syncLoadsCdc(listOf(sample("t-abc")), null, playFeedback = false)
        assertEquals(SyncStatus.DUPLICATE, result.status)
        assertEquals(0, result.addedCount)
        assertEquals(normalizeTripId("t-abc"), normalizeTripId("T-ABC"))
    }

    @Test
    fun cdc_emptyWhenInvalid() = runBlocking {
        val repo = repo()
        val bad = sample("T-UNKNOWN", rate = 0.0)
        val result = repo.syncLoadsCdc(listOf(bad), null, playFeedback = false)
        assertEquals(SyncStatus.EMPTY, result.status)
    }
}
