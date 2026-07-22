package com.truckerload.data.local

import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getLastDeliveryMillis

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
        route = route,
        firstPuCityState = firstPuCityState,
        lastDelCityState = lastDelCityState,
        durationDays = durationDays,
        pace = pace,
        stopCount = stopCount,
        isDispute = isDispute,
        disputeResponseDate = disputeResponseDate,
        disputeCompleted = disputeCompleted,
        actualFinishDate = actualFinishDate,
        stops = stops.map { it.toDomain() },
        penalties = penalties.map { it.toDomain() }
    )

fun Load.toEntity(): LoadEntity {
    val metrics = withRouteMetrics()
    return LoadEntity(
        id = metrics.id,
        tripId = metrics.tripId,
        date = metrics.date,
        totalRate = metrics.totalRate,
        totalMiles = metrics.totalMiles,
        pointA = metrics.pointA,
        pointB = metrics.pointB,
        puCount = metrics.puCount,
        delCount = metrics.delCount,
        weekNumber = metrics.weekNumber,
        year = metrics.year,
        rawMessage = metrics.rawMessage,
        parsedAt = metrics.parsedAt,
        updatedAt = metrics.updatedAt,
        firstPuMillis = getFirstPickUpMillis(metrics),
        lastDelMillis = LoadYieldCalculator.resolveFinishMillis(metrics)
            ?: getLastDeliveryMillis(metrics),
        route = metrics.route,
        firstPuCityState = metrics.firstPuCityState,
        lastDelCityState = metrics.lastDelCityState,
        durationDays = metrics.durationDays,
        pace = metrics.pace,
        stopCount = metrics.stopCount,
        isDispute = metrics.isDispute,
        disputeResponseDate = metrics.disputeResponseDate,
        disputeCompleted = metrics.disputeCompleted,
        actualFinishDate = metrics.actualFinishDate,
    )
}

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
