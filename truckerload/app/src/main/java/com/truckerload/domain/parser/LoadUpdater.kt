package com.truckerload.domain.parser

import android.util.Log
import com.truckerload.data.local.entities.LoadHistory
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.utils.withReportingWeek

class LoadUpdater(
    private val loadRepository: LoadRepository,
) {
    suspend fun updateLoad(
        oldLoad: Load,
        newData: Load,
        changes: List<String>,
    ): Load {
        changes.forEach { change ->
            Log.d(TAG, "Изменение: $change")
        }

        val now = System.currentTimeMillis()
        val updatedLoad = oldLoad.copy(
            totalRate = newData.totalRate,
            totalMiles = newData.totalMiles,
            stops = newData.stops.map { stop ->
                stop.copy(loadId = oldLoad.id)
            },
            penalties = newData.penalties.map { penalty ->
                penalty.copy(loadId = oldLoad.id)
            },
            rawMessage = newData.rawMessage,
            updatedAt = now,
        ).withReportingWeek()

        loadRepository.update(updatedLoad)
        recordHistory(oldLoad, newData, changes, now)
        return updatedLoad
    }

    private suspend fun recordHistory(
        oldLoad: Load,
        newData: Load,
        changes: List<String>,
        timestamp: Long,
    ) {
        if (changes.any { it.startsWith("totalRate") }) {
            loadRepository.addChangeHistory(
                LoadHistory(
                    loadId = oldLoad.id,
                    field = "totalRate",
                    oldValue = oldLoad.totalRate.toString(),
                    newValue = newData.totalRate.toString(),
                    timestamp = timestamp,
                )
            )
        }
        if (changes.any { it.startsWith("totalMiles") }) {
            loadRepository.addChangeHistory(
                LoadHistory(
                    loadId = oldLoad.id,
                    field = "totalMiles",
                    oldValue = oldLoad.totalMiles.toString(),
                    newValue = newData.totalMiles.toString(),
                    timestamp = timestamp,
                )
            )
        }
        changes.filter { it.startsWith("stop") }.forEach { change ->
            loadRepository.addChangeHistory(
                LoadHistory(
                    loadId = oldLoad.id,
                    field = change.substringBefore(":"),
                    oldValue = oldLoad.stopCount.toString(),
                    newValue = newData.stopCount.toString(),
                    timestamp = timestamp,
                )
            )
        }
    }

    companion object {
        private const val TAG = "LoadUpdater"
    }
}
