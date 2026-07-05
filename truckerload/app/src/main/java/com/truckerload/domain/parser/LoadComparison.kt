package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getLastDeliveryMillis
import kotlin.math.abs

data class LoadComparison(
    val tripIdMatch: Boolean,
    val totalRateMatch: Boolean,
    val totalMilesMatch: Boolean,
    val stopCountMatch: Boolean,
    val firstPuTimeMatch: Boolean,
    val lastDelTimeMatch: Boolean,
    val stopsHashMatch: Boolean,
    val rawMessageMatch: Boolean,
) {
    fun isIdentical(): Boolean =
        totalRateMatch &&
            totalMilesMatch &&
            stopCountMatch &&
            firstPuTimeMatch &&
            lastDelTimeMatch &&
            stopsHashMatch

    fun hasMinorChanges(): Boolean {
        val changedFields = listOf(!totalRateMatch, !totalMilesMatch).count { it }
        return changedFields in 1..2 &&
            stopCountMatch &&
            firstPuTimeMatch &&
            lastDelTimeMatch &&
            stopsHashMatch
    }

    fun hasMajorChanges(): Boolean =
        !stopCountMatch ||
            !firstPuTimeMatch ||
            !lastDelTimeMatch ||
            !stopsHashMatch
}

fun compareLoads(
    old: Load,
    new: Load,
    priceThresholdPercent: Double = 0.0,
): LoadComparison {
    val detector = LoadChangeDetector
    val oldFirstPu = getFirstPickUpMillis(old)
    val newFirstPu = getFirstPickUpMillis(new)
    val oldLastDel = getLastDeliveryMillis(old)
    val newLastDel = getLastDeliveryMillis(new)

    val rateMatch = if (priceThresholdPercent > 0.0) {
        !detector.isRateChangedSignificant(old.totalRate, new.totalRate, priceThresholdPercent)
    } else {
        abs(old.totalRate - new.totalRate) < 0.01
    }

    return LoadComparison(
        tripIdMatch = old.tripId.equals(new.tripId, ignoreCase = true),
        totalRateMatch = rateMatch,
        totalMilesMatch = !detector.isMilesChanged(old.totalMiles, new.totalMiles),
        stopCountMatch = !detector.isStopCountChanged(old.stops.size, new.stops.size),
        firstPuTimeMatch = when {
            oldFirstPu == null && newFirstPu == null -> true
            oldFirstPu == null || newFirstPu == null -> false
            else -> !detector.isFirstPuTimeChanged(oldFirstPu, newFirstPu)
        },
        lastDelTimeMatch = when {
            oldLastDel == null && newLastDel == null -> true
            oldLastDel == null || newLastDel == null -> false
            else -> !detector.isLastDelTimeChanged(oldLastDel, newLastDel)
        },
        stopsHashMatch = StopsHasher.calculateStopsHash(old.stops) ==
            StopsHasher.calculateStopsHash(new.stops),
        rawMessageMatch = old.rawMessage == new.rawMessage,
    )
}
