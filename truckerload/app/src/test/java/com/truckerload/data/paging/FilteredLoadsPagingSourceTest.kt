package com.truckerload.data.paging

import androidx.paging.PagingSource
import com.truckerload.domain.model.Load
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilteredLoadsPagingSourceTest {
    @Test
    fun pagesFilteredLoads() = runBlocking {
        val loads = (0 until 90).map { i ->
            Load(
                id = "id-$i",
                tripId = "T-$i",
                date = "2026-07-01",
                totalRate = 1.0,
                totalMiles = 1.0,
                pointA = "A",
                pointB = "B",
                puCount = 1,
                delCount = 1,
                weekNumber = 27,
                year = 2026,
                rawMessage = "",
                parsedAt = i.toLong(),
                updatedAt = i.toLong(),
            )
        }
        val source = FilteredLoadsPagingSource(loads)
        val page0 = source.load(PagingSource.LoadParams.Refresh(key = 0, loadSize = 40, placeholdersEnabled = false))
            as PagingSource.LoadResult.Page
        assertEquals(40, page0.data.size)
        assertNull(page0.prevKey)
        assertEquals(1, page0.nextKey)

        val page2 = source.load(PagingSource.LoadParams.Append(key = 2, loadSize = 40, placeholdersEnabled = false))
            as PagingSource.LoadResult.Page
        assertEquals(10, page2.data.size)
        assertNull(page2.nextKey)
    }
}
