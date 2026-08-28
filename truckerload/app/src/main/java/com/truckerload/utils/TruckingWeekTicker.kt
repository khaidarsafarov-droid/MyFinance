package com.truckerload.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive

/**
 * Shared trucking-week clock for all [com.truckerload.data.repository.LoadRepository] collectors.
 * One timer instead of one [kotlinx.coroutines.flow.flow] loop per subscriber.
 */
object TruckingWeekTicker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentWeek: SharedFlow<Pair<Int, Int>> = flow {
        emit(getCurrentWeekNumberAndYear())
        while (currentCoroutineContext().isActive) {
            delay(60_000L)
            emit(getCurrentWeekNumberAndYear())
        }
    }
        .distinctUntilChanged()
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 90_000L),
            replay = 1,
        )
}
