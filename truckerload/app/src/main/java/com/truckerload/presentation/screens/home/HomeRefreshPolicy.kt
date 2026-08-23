package com.truckerload.presentation.screens.home

import com.truckerload.domain.model.Load

/**
 * Pull-to-refresh display rules for Home.
 *
 * Refresh must never replace a populated journal with an empty/skeleton frame.
 * Room can emit `emptyList()` for a moment on invalidation or resubscribe;
 * keep the last snapshot until a real list arrives or refresh finishes.
 */
internal object HomeRefreshPolicy {
    const val MIN_INDICATOR_MS = 300L

    fun retainLoads(
        incoming: List<Load>,
        previous: List<Load>,
        isRefreshing: Boolean,
    ): List<Load> {
        if (isRefreshing && incoming.isEmpty() && previous.isNotEmpty()) {
            return previous
        }
        return incoming
    }

    /** Hide the empty-journal CTA while Paging is still loading the first page. */
    fun shouldShowEmptyJournal(
        visibleItemCount: Int,
        pagingRefreshLoading: Boolean,
    ): Boolean {
        if (visibleItemCount > 0) return false
        if (pagingRefreshLoading) return false
        return true
    }

    /** Initial skeletons must not cover an in-place pull-to-refresh. */
    fun shouldShowInitialOverlay(
        isInitialLoading: Boolean,
        isRefreshing: Boolean,
    ): Boolean = isInitialLoading && !isRefreshing
}
