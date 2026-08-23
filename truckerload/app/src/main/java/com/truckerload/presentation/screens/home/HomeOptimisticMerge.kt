package com.truckerload.presentation.screens.home

import com.truckerload.domain.model.Load

/** Apply optimistic overlay + pending deletes on top of a Room snapshot. */
internal fun mergeLoadsWithOptimisticOverlay(
    loads: List<Load>,
    overlay: Map<String, Load>,
    pendingDeletes: Set<String>,
): List<Load> {
    val base = loads
        .filter { it.id !in pendingDeletes }
        .map { overlay[it.id] ?: it }
    val loadIds = loads.map { it.id }.toSet()
    val newLoads = overlay.values.filter { it.id !in loadIds && it.id !in pendingDeletes }
    return base + newLoads
}
