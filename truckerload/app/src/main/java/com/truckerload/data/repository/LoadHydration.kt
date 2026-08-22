package com.truckerload.data.repository

import com.truckerload.data.local.dao.PenaltyDao
import com.truckerload.data.local.dao.StopDao
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.toDomain
import com.truckerload.domain.model.Load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

/**
 * Attach stops/penalties for UI. Dates stay as stored — [com.truckerload.utils.LoadDateRepair]
 * runs on write and session repair, not on every Room emission (that froze Home on the main thread).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<List<LoadEntity>>.hydrateOnIo(
    stopDao: StopDao,
    penaltyDao: PenaltyDao,
): Flow<List<Load>> =
    mapLatest { entities -> hydrateLoadEntities(entities, stopDao, penaltyDao) }
        .flowOn(Dispatchers.IO)
        .conflate()

internal suspend fun hydrateLoadEntities(
    entities: List<LoadEntity>,
    stopDao: StopDao,
    penaltyDao: PenaltyDao,
): List<Load> {
    if (entities.isEmpty()) return emptyList()
    val loadIds = entities.map { it.id }
    // SQLite caps IN(...) at ~999 parameters — batch large fleets.
    val stopsByLoadId = loadIds.chunked(500)
        .flatMap { chunk -> stopDao.getStopsByLoadIds(chunk) }
        .groupBy { it.loadId }
    val penaltiesByLoadId = loadIds.chunked(500)
        .flatMap { chunk -> penaltyDao.getPenaltiesByLoadIds(chunk) }
        .groupBy { it.loadId }
    return entities.map { entity ->
        entity.toDomain(
            stops = stopsByLoadId[entity.id].orEmpty(),
            penalties = penaltiesByLoadId[entity.id].orEmpty(),
        )
    }
}
