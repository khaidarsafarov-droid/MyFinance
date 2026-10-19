package com.truckerload.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.domain.money.Money
import kotlin.math.ceil

/**
 * Fields the journal card actually paints, plus the ids the row needs for
 * click, swipe, camera, and shared-bounds. Built once per [Load] equality.
 */
@Immutable
data class LoadCardModel(
    val id: String,
    val tripId: String,
    val date: String,
    val parsedAt: Long,
    val headerDate: String,
    val totalRate: Money,
    val pace: Money,
    val drivenMiles: Double,
    val rpm: Double?,
    val rpmTier: RpmTier?,
    val isDispute: Boolean,
    val hadDispute: Boolean,
    val isActiveDispute: Boolean,
    val originCity: String,
    val destCity: String,
    val originStreet: String,
    val destStreet: String,
    val stopCount: Int,
    val durationDays: Int,
    val source: LoadSourceTag,
    val route: String,
)

fun Load.toCardModel(minProfitRpm: Double, targetProfitRpm: Double): LoadCardModel {
    val miles = drivenMiles
    val rpm = computeRpm(totalRate, miles)
    val pu = stops.firstOrNull { it.type == StopType.PU }
    val del = stops.lastOrNull { it.type == StopType.DEL }
    val originCity = firstPuCityState.ifBlank { pointA }.ifBlank { "—" }
    val destCity = lastDelCityState.ifBlank { pointB }.ifBlank { "—" }
    return LoadCardModel(
        id = id,
        tripId = tripId,
        date = date,
        parsedAt = parsedAt,
        headerDate = effectiveFinishDate() ?: date,
        totalRate = totalRate,
        pace = pace,
        drivenMiles = miles,
        rpm = rpm,
        rpmTier = rpmTier(rpm, minProfitRpm, targetProfitRpm),
        isDispute = isDispute,
        hadDispute = hadDispute,
        isActiveDispute = isActiveDispute,
        originCity = originCity,
        destCity = destCity,
        originStreet = routeStreet(pu, originCity),
        destStreet = routeStreet(del, destCity),
        stopCount = stopCount.takeIf { it > 0 } ?: (puCount + delCount),
        durationDays = ceil(durationDays).toInt().coerceAtLeast(if (durationDays > 0.0) 1 else 0),
        source = journalSource(),
        route = formatLoadRoute(this),
    )
}

@Composable
internal fun rememberLoadCard(load: Load, thresholds: RpmThresholds): LoadCardModel =
    remember(load, thresholds.minProfit, thresholds.targetProfit) {
        load.toCardModel(thresholds.minProfit, thresholds.targetProfit)
    }

@Composable
internal fun rememberLoadCards(loads: List<Load>, thresholds: RpmThresholds): List<LoadCardModel> =
    remember(loads, thresholds.minProfit, thresholds.targetProfit) {
        loads.map { it.toCardModel(thresholds.minProfit, thresholds.targetProfit) }
    }
