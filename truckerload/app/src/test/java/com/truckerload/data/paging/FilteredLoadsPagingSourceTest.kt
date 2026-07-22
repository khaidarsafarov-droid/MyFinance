package com.truckerload.data.paging

import androidx.paging.PagingSource
import com.truckerload.domain.model.Load
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilteredLoadsPagingSourceTest {

    private fun load(id: String) = Load(
        id = id,
        tripId = id,
        date = "2026-01-01",
        totalRate = 100.0,
        totalMiles = 50.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 1,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun emptyList_returnsEmptyPage() = runTest {
        val source = FilteredLoadsPagingSource(emptyList())
        val page = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertTrue(page.data.isEmpty())
        assertNull(page.prevKey)
        assertNull(page.nextKey)
    }

    @Test
    fun pagesBoundaries() = runTest {
        val loads = (0 until 5).map { load("T$it") }
        val source = FilteredLoadsPagingSource(loads)
        val first = source.load(
            PagingSource.LoadParams.Refresh(key = 0, loadSize = 2, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertEquals(listOf("T0", "T1"), first.data.map { it.id })
        assertEquals(1, first.nextKey)

        val second = source.load(
            PagingSource.LoadParams.Append(key = 1, loadSize = 2, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertEquals(listOf("T2", "T3"), second.data.map { it.id })
        assertEquals(0, second.prevKey)
        assertEquals(2, second.nextKey)

        val last = source.load(
            PagingSource.LoadParams.Append(key = 2, loadSize = 2, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertEquals(listOf("T4"), last.data.map { it.id })
        assertNull(last.nextKey)
    }
}
