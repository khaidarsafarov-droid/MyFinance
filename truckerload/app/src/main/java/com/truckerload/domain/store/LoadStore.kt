package com.truckerload.domain.store

import com.truckerload.domain.money.Money
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop

/**
 * Load journal the domain use cases are allowed to touch.
 * Room implements this in the data layer.
 */
interface LoadStore {
    suspend fun getByTripId(tripId: String): Load?

    suspend fun getByRouteAndDate(
        origin: String,
        destination: String,
        date: String,
        rate: Money,
    ): Load?

    suspend fun getByStops(stops: List<Stop>, date: String): Load?

    suspend fun insertLoad(
        load: Load,
        playFeedback: Boolean = true,
        reviveDeleted: Boolean = false,
    )

    suspend fun update(load: Load)

    suspend fun addChangeHistory(change: LoadFieldChange)

    suspend fun <T> runBatchWrite(block: suspend () -> T): T

    suspend fun refreshReportingWeeks()

    suspend fun getAllLoadsOnce(): List<Load>

    suspend fun deleteLoad(loadId: String)

    suspend fun loadsForWeek(weekNumber: Int, year: Int): List<Load>
}
