package com.truckerload.data.paging

import androidx.paging.PagingSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.preferences.AuthStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device smoke: Room SQL [PagingSource] returns paged load rows for week filter.
 */
@RunWith(AndroidJUnit4::class)
class RoomLoadPagingInstrumentedTest {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val userId = "paging_smoke_user"

    @Before
    fun setUp() {
        AppDatabase.closeCurrent()
        AuthStore(context).logout()
        deleteDbFiles()
    }

    @After
    fun tearDown() {
        AppDatabase.closeCurrent()
        AuthStore(context).logout()
        deleteDbFiles()
    }

    @Test
    fun pagingLoadsByWeek_returnsInsertedRows() = runBlocking {
        AuthStore(context).login(userId, "paging@smoke.test", rememberMe = true)
        val db = AppDatabase.getInstance(context, userId)
        val dao = db.loadDao()
        repeat(5) { i ->
            dao.insert(
                LoadEntity(
                    id = "page-load-$i",
                    tripId = "TRIP-$i",
                    date = "2026-07-20",
                    totalRate = 1000.0 + i,
                    totalMiles = 100.0,
                    pointA = "A$i",
                    pointB = "B$i",
                    puCount = 1,
                    delCount = 1,
                    weekNumber = 30,
                    year = 2026,
                    rawMessage = "",
                    parsedAt = 1_000L + i,
                    updatedAt = 1_000L + i,
                ),
            )
        }

        val source = dao.pagingLoadsByWeek(30, 2026)
        val page = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 2,
                placeholdersEnabled = false,
            ),
        ) as PagingSource.LoadResult.Page
        assertEquals(2, page.data.size)
        assertTrue(page.data.all { it.weekNumber == 30 && it.year == 2026 })

        val nextKey = page.nextKey
        assertTrue(nextKey != null)
        val next = source.load(
            PagingSource.LoadParams.Append(
                key = nextKey!!,
                loadSize = 2,
                placeholdersEnabled = false,
            ),
        ) as PagingSource.LoadResult.Page
        assertEquals(2, next.data.size)
    }

    private fun deleteDbFiles() {
        val name = AppDatabase.databaseNameFor(userId)
        val base = context.getDatabasePath(name)
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            java.io.File(base.path + suffix).delete()
        }
    }
}
