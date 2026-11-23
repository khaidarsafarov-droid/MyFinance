package com.truckerload.data.repository

import com.truckerload.data.local.entities.LoadHistory
import com.truckerload.domain.money.Money
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.store.LoadFieldChange
import com.truckerload.domain.store.LoadStore
import kotlinx.coroutines.flow.first

/** Room-backed [LoadStore]. History rows stay in the data layer. */
class RoomLoadStore(
    private val loads: LoadRepository,
) : LoadStore {
    override suspend fun getByTripId(tripId: String): Load? = loads.getByTripId(tripId)

    override suspend fun getByRouteAndDate(
        origin: String,
        destination: String,
        date: String,
        rate: Money,
    ): Load? = loads.getByRouteAndDate(origin, destination, date, rate)

    override suspend fun getByStops(stops: List<Stop>, date: String): Load? =
        loads.getByStops(stops, date)

    override suspend fun insertLoad(load: Load, playFeedback: Boolean, reviveDeleted: Boolean) {
        loads.insertLoad(load, playFeedback, reviveDeleted)
    }

    override suspend fun update(load: Load) {
        loads.update(load)
    }

    override suspend fun addChangeHistory(change: LoadFieldChange) {
        loads.addChangeHistory(
            LoadHistory(
                loadId = change.loadId,
                field = change.field,
                oldValue = change.oldValue,
                newValue = change.newValue,
                timestamp = change.timestamp,
            ),
        )
    }

    override suspend fun <T> runBatchWrite(block: suspend () -> T): T = loads.runBatchWrite(block)

    override suspend fun refreshReportingWeeks() {
        loads.refreshReportingWeeks()
    }

    override suspend fun getAllLoadsOnce(): List<Load> = loads.getAllLoadsOnce()

    override suspend fun deleteLoad(loadId: String) {
        loads.deleteLoad(loadId)
    }

    override suspend fun loadsForWeek(weekNumber: Int, year: Int): List<Load> =
        loads.getLoadsByWeek(weekNumber, year).first()
}
