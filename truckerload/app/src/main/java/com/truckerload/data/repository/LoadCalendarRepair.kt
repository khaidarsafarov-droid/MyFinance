package com.truckerload.data.repository

import androidx.room.withTransaction
import com.truckerload.data.local.AppDatabase
import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.utils.LoadDateRepair
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getLastDeliveryMillis
import com.truckerload.utils.withReportingWeek

internal suspend fun persistRepairedLoadDates(db: AppDatabase): Int {
    val loadDao = db.loadDao()
    val stopDao = db.stopDao()
    val penaltyDao = db.penaltyDao()
    val entities = loadDao.getAllLoadsOnce()
    if (entities.isEmpty()) return 0
    val now = System.currentTimeMillis()
    val updates = hydrateLoadEntities(entities, stopDao, penaltyDao).mapNotNull { load ->
        // FIX: anchor repair to parsedAt (Telegram/message time), not wall clock at session start
        val repaired = LoadDateRepair.repair(
            load = load,
            referenceMillis = load.parsedAt.takeIf { it >= LoadDateRepair.MIN_SANE_REFERENCE_MS },
        )
        repaired.takeIf {
            it.date != load.date ||
                it.weekNumber != load.weekNumber ||
                it.year != load.year
        }
    }
    if (updates.isEmpty()) return 0
    db.withTransaction {
        for (load in updates) {
            val dated = load.withReportingWeek().withRouteMetrics()
            loadDao.updateCalendarFields(
                loadId = dated.id,
                loadDate = dated.date,
                weekNumber = dated.weekNumber,
                year = dated.year,
                updatedAt = now,
                firstPuMillis = getFirstPickUpMillis(dated),
                lastDelMillis = LoadYieldCalculator.resolveFinishMillis(dated)
                    ?: getLastDeliveryMillis(dated),
                durationDays = dated.durationDays,
                pace = dated.pace,
            )
        }
    }
    return updates.size
}
