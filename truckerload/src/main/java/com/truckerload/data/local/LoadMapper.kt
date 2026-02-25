package com.truckerload.data.local

import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType

fun LoadEntity.toDomain(stops: List<StopEntity> = emptyList(), penalties: List<PenaltyEntity> = emptyList()): Load =
    Load(
        id = id,
        tripId = tripId,
        date = date,
        totalRate = totalRate,
        totalMiles = totalMiles,
        pointA = pointA,
        pointB = pointB,
        puCount = puCount,
        delCount = delCount,
        weekNumber = weekNumber,
        year = year,
        rawMessage = rawMessage,
        parsedAt = parsedAt,
        updatedAt = updatedAt,
        stops = stops.map { it.toDomain() },
        penalties = penalties.map { it.toDomain() }
    )

fun Load.toEntity(): LoadEntity =
    LoadEntity(
        id = id,
        tripId = tripId,
        date = date,
        totalRate = totalRate,
        totalMiles = totalMiles,
        pointA = pointA,
        pointB = pointB,
        puCount = puCount,
        delCount = delCount,
        weekNumber = weekNumber,
        year = year,
        rawMessage = rawMessage,
        parsedAt = parsedAt,
        updatedAt = updatedAt
    )

fun StopEntity.toDomain(): Stop =
    Stop(
        id = id,
        loadId = loadId,
        stopNumber = stopNumber,
        type = if (type.equals("PU", ignoreCase = true)) StopType.PU else StopType.DEL,
        puNumber = puNumber,
        note = note,
        scheduledTime = scheduledTime,
        timezone = timezone,
        facilityCode = facilityCode,
        fullAddress = fullAddress,
        city = city,
        state = state,
        zip = zip
    )

fun Stop.toEntity(loadId: String): StopEntity =
    StopEntity(
        id = id,
        loadId = loadId,
        stopNumber = stopNumber,
        type = type.name,
        puNumber = puNumber,
        note = note,
        scheduledTime = scheduledTime,
        timezone = timezone,
        facilityCode = facilityCode,
        fullAddress = fullAddress,
        city = city,
        state = state,
        zip = zip
    )

fun PenaltyEntity.toDomain(): Penalty =
    Penalty(id = id, loadId = loadId, description = description, amount = amount)

fun Penalty.toEntity(loadId: String): PenaltyEntity =
    PenaltyEntity(id = id, loadId = loadId, description = description, amount = amount)
