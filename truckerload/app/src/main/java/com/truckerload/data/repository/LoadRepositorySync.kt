package com.truckerload.data.repository

import androidx.room.withTransaction
import com.truckerload.data.local.AppDatabase
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.normalizeTripId
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.parser.LoadProcessor
import com.truckerload.domain.parser.ProcessingResult
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.LoadDateRepair
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.withReportingWeek

data class SyncLoadsResult(
    val addedCount: Int,
    val lastAddedText: String,
    val status: SyncStatus,
)

enum class SyncStatus { SUCCESS, DUPLICATE, EMPTY, UPDATED }

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

    val loadRepository = LoadRepository(db)
    val processor = LoadProcessor(loadRepository)
    var addedCount = 0
    var updatedCount = 0
    var lastText = ""

    db.withTransaction {
        for (load in validLoads) {
            val normalizedTripId = normalizeTripId(load.tripId)
            val now = System.currentTimeMillis()
            val parsedAt = messageDateSeconds?.times(1000) ?: load.parsedAt.takeIf { it > 0L } ?: now
            val messageYear = messageDateSeconds
                ?.let { formatDateFromUnixSeconds(it).take(4).toIntOrNull() }
            val prepared = load.copy(tripId = normalizedTripId, parsedAt = parsedAt)
            val repaired = LoadDateRepair.repair(prepared, messageYear, parsedAt)
            val dated = when {
                repaired.date.isBlank() && messageDateSeconds != null ->
                    repaired.copy(date = formatDateFromUnixSeconds(messageDateSeconds))
                else -> repaired
            }
            val normalized = dated.withReportingWeek().withRouteMetrics().copy(
                parsedAt = parsedAt,
                updatedAt = now,
            )

            when (
                val outcome = processor.processLoad(
                    parsedLoad = normalized,
                    messageDateSeconds = messageDateSeconds,
                    playFeedback = false,
                )
            ) {
                ProcessingResult.Added -> {
                    addedCount++
                    lastText = formatLoadSummary(normalized)
                }
                is ProcessingResult.Updated -> {
                    updatedCount++
                    lastText = formatLoadSummary(normalized)
                }
                is ProcessingResult.Replaced -> {
                    updatedCount++
                    lastText = formatLoadSummary(normalized)
                }
                is ProcessingResult.Skipped -> Unit
            }
        }
    }

    val total = addedCount + updatedCount
    if (total == 0) {
        return SyncLoadsResult(0, "", SyncStatus.DUPLICATE)
    }
    if (playFeedback && addedCount > 0) {
        FeedbackManager.onLoadAdded()
    }
    onPersisted()
    return SyncLoadsResult(
        addedCount = total,
        lastAddedText = lastText,
        status = if (updatedCount > 0 && addedCount == 0) SyncStatus.UPDATED else SyncStatus.SUCCESS,
    )
}

private fun formatLoadSummary(load: Load): String =
    "${load.tripId} — ${load.pointA} → ${load.pointB}, $${String.format("%,.2f", load.totalRate)}"
