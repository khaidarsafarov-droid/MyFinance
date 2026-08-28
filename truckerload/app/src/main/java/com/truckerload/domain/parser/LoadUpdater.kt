package com.truckerload.domain.parser

import android.util.Log
import com.truckerload.data.local.entities.LoadHistory
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.withRouteMetrics
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
            Log.d(TAG, "Change: $change")
        }

        val now = System.currentTimeMillis()
        // Синхронизируем маршрут/даты из новых стопов, затем пересчитываем метрики —
        // иначе карточки в журнале показывают устаревший маршрут после Telegram-обновления.
        val updatedLoad = oldLoad.copy(
            tripId = newData.tripId,
            date = newData.date,
            totalRate = newData.totalRate,
            totalMiles = newData.totalMiles,
            pointA = newData.pointA,
            pointB = newData.pointB,
            stops = newData.stops.map { stop ->
                stop.copy(loadId = oldLoad.id)
            },
            penalties = newData.penalties.map { penalty ->
                penalty.copy(loadId = oldLoad.id)
            },
            rawMessage = newData.rawMessage,
            updatedAt = now,
            // Keep driver finish override across Relay/Telegram sync updates.
            actualFinishDate = oldLoad.actualFinishDate,
        ).withReportingWeek().withRouteMetrics()

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
            val field = change.substringBefore(":")
            val (oldValue, newValue) = when {
                "→" in change -> {
                    val detail = change.substringAfter(":", missingDelimiterValue = change)
                    detail.substringBefore("→").trim() to
                        detail.substringAfter("→", missingDelimiterValue = detail).trim()
                }
                field == "stopCount" ->
                    oldLoad.stopCount.toString() to newData.stopCount.toString()
                field == "stopAddress" ->
                    summarizeStops(oldLoad) to summarizeStops(newData)
                field == "stopStatus" ->
                    summarizeStopStatus(oldLoad) to summarizeStopStatus(newData)
                else -> change to change
            }
            loadRepository.addChangeHistory(
                LoadHistory(
                    loadId = oldLoad.id,
                    field = field,
                    oldValue = oldValue,
                    newValue = newValue,
                    timestamp = timestamp,
                ),
            )
        }
    }

    private fun summarizeStops(load: Load): String =
        load.stops.joinToString(" | ") { stop ->
            stop.fullAddress.ifBlank { "${stop.city}, ${stop.state}".trim(',', ' ') }
        }

    private fun summarizeStopStatus(load: Load): String =
        load.stops.joinToString(" | ") { stop ->
            val note = stop.note.orEmpty()
            if (note.contains("CANCEL", ignoreCase = true)) "CANCELLED" else "ACTIVE"
        }

    companion object {
        private const val TAG = "LoadUpdater"
    }
}
