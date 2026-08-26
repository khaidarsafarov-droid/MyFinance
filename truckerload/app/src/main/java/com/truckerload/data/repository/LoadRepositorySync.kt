package com.truckerload.data.repository

import androidx.room.withTransaction
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toEntity
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.normalizeTripId
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.LoadDateRepair
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.withReportingWeek

data class SyncLoadsResult(
    val addedCount: Int,
    val lastAddedText: String,
    val status: SyncStatus,
)

enum class SyncStatus { SUCCESS, DUPLICATE, EMPTY }

internal suspend fun importLoadsIfNotDuplicateImpl(
    loadDao: com.truckerload.data.local.dao.LoadDao,
    runBatchWrite: suspend (suspend () -> Unit) -> Unit,
    insertLoad: suspend (Load, Boolean) -> Unit,
    loads: List<Load>,
    parsedCount: Int,
): com.truckerload.utils.LoadImporter.ImportResult {
    val incomingTripIds = loads.map { normalizeTripId(it.tripId) }.filter { it.isNotBlank() }
    var imported = 0
    var skipped = 0
    runBatchWrite {
        val existingTripIds = loadDao.getExistingTripIds(incomingTripIds)
            .map { normalizeTripId(it) }
            .toMutableSet()
        for (load in loads) {
            val tripId = normalizeTripId(load.tripId)
            if (tripId in existingTripIds) {
                skipped++
                continue
            }
            existingTripIds.add(tripId)
            insertLoad(load.copy(tripId = tripId), false)
            imported++
        }
    }
    if (imported > 0) {
        FeedbackManager.onLoadAdded()
    }
    return com.truckerload.utils.LoadImporter.ImportResult(
        imported = imported,
        skipped = skipped,
        parsed = parsedCount,
    )
}

internal suspend fun syncLoadsCdcImpl(
    db: AppDatabase,
    loadDao: com.truckerload.data.local.dao.LoadDao,
    stopDao: com.truckerload.data.local.dao.StopDao,
    incomingLoads: List<Load>,
    messageDateSeconds: Long?,
    playFeedback: Boolean,
    onPersisted: () -> Unit,
): SyncLoadsResult {
    val validLoads = incomingLoads.filter { load ->
        load.tripId.isNotBlank() && load.tripId != "T-UNKNOWN" &&
            (load.pointA.isNotBlank() || load.pointB.isNotBlank()) && load.totalRate > 0
    }
    if (validLoads.isEmpty()) {
        return SyncLoadsResult(0, "", SyncStatus.EMPTY)
    }

    val tripIds = validLoads.map { normalizeTripId(it.tripId) }
    var result = SyncLoadsResult(0, "", SyncStatus.DUPLICATE)

    db.withTransaction {
        val existingIds = loadDao.getExistingTripIds(tripIds).map { normalizeTripId(it) }.toSet()
        val toInsert = validLoads.filter { normalizeTripId(it.tripId) !in existingIds }
        if (toInsert.isEmpty()) return@withTransaction

        val now = System.currentTimeMillis()
        val parsedAt = messageDateSeconds?.times(1000) ?: now
        val messageYear = messageDateSeconds
            ?.let { formatDateFromUnixSeconds(it).take(4).toIntOrNull() }
        val loadEntities = mutableListOf<LoadEntity>()
        val stopEntities = mutableListOf<StopEntity>()

        for (load in toInsert) {
            val normalized = load.copy(tripId = normalizeTripId(load.tripId), parsedAt = parsedAt)
            val repaired = LoadDateRepair.repair(normalized, messageYear, parsedAt)
            val dated = when {
                repaired.date.isBlank() && messageDateSeconds != null ->
                    repaired.copy(date = formatDateFromUnixSeconds(messageDateSeconds))
                else -> repaired
            }
            val loadWithWeek = dated.withReportingWeek().withRouteMetrics().copy(
                parsedAt = parsedAt,
                updatedAt = now,
            )
            loadEntities.add(loadWithWeek.toEntity())
            stopEntities.addAll(loadWithWeek.stops.map { it.toEntity(loadWithWeek.id) })
        }

        loadDao.insertAll(loadEntities)
        if (stopEntities.isNotEmpty()) stopDao.insertAll(stopEntities)

        val lastAdded = toInsert.last()
        val lastAddedText =
            "${lastAdded.tripId} — ${lastAdded.pointA} → ${lastAdded.pointB}, $${String.format("%,.2f", lastAdded.totalRate)}"
        result = SyncLoadsResult(toInsert.size, lastAddedText, SyncStatus.SUCCESS)
    }

    if (result.status == SyncStatus.SUCCESS) {
        onPersisted()
        if (playFeedback) {
            FeedbackManager.onLoadAdded()
        }
    }
    return result
}
