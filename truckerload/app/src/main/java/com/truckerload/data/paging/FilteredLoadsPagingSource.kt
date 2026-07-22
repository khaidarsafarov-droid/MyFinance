package com.truckerload.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.truckerload.domain.model.Load

/**
 * In-memory PagingSource over an already-filtered load list.
 * Used by Home when Room returns the full set but the UI should window rows.
 */
class FilteredLoadsPagingSource(
    private val loads: List<Load>,
) : PagingSource<Int, Load>() {

    override fun getRefreshKey(state: PagingState<Int, Load>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor)
        return page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Load> {
        val page = params.key ?: 0
        val pageSize = params.loadSize.coerceAtLeast(1)
        val from = page * pageSize
        if (from >= loads.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = if (page > 0) page - 1 else null,
                nextKey = null,
            )
        }
        val to = (from + pageSize).coerceAtMost(loads.size)
        return LoadResult.Page(
            data = loads.subList(from, to),
            prevKey = if (page > 0) page - 1 else null,
            nextKey = if (to < loads.size) page + 1 else null,
        )
    }
}
