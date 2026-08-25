package com.truckerload.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.SwipeableLoadCard
import com.truckerload.presentation.utils.adaptiveHorizontalPadding

/** Flat list rows for multi-column home journal (headers full-width, loads chunked). */
sealed class HomeGridRow {
    data class FullWidth(val item: HomeListItem) : HomeGridRow()
    data class Loads(val loads: List<Load>) : HomeGridRow()
}

/**
 * Groups consecutive [HomeListItem.LoadItem]s into rows of [columns]; headers stay full-width.
 */
fun buildHomeGridRows(items: List<HomeListItem>, columns: Int): List<HomeGridRow> {
    require(columns >= 1)
    if (columns == 1) {
        return items.map { item ->
            when (item) {
                is HomeListItem.LoadItem -> HomeGridRow.Loads(listOf(item.load))
                else -> HomeGridRow.FullWidth(item)
            }
        }
    }
    val rows = mutableListOf<HomeGridRow>()
    val pending = mutableListOf<Load>()
    fun flush() {
        if (pending.isEmpty()) return
        rows += HomeGridRow.Loads(pending.toList())
        pending.clear()
    }
    for (item in items) {
        when (item) {
            is HomeListItem.LoadItem -> {
                pending += item.load
                if (pending.size >= columns) flush()
            }
            is HomeListItem.FilteredSectionHeader -> Unit
            else -> {
                flush()
                rows += HomeGridRow.FullWidth(item)
            }
        }
    }
    flush()
    return rows
}

/** Number of LazyColumn rows for a paged load list at [columns] per row. */
fun pagedLoadRowCount(itemCount: Int, columns: Int): Int {
    if (itemCount <= 0) return 0
    val cols = columns.coerceAtLeast(1)
    return (itemCount + cols - 1) / cols
}

@Composable
internal fun HomeLoadCardRow(
    loads: List<Load>,
    columns: Int,
    rpmThresholds: RpmThresholds,
    settleKey: Any,
    onLoadClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onLoadCamera: (loadId: String, tripId: String, loadDate: String) -> Unit,
    onLoadScan: (loadId: String, tripId: String, loadDate: String) -> Unit,
) {
    val horizontal = adaptiveHorizontalPadding()
    val enableSwipe = columns == 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontal),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        loads.forEach { load ->
            SwipeableLoadCard(
                load = load,
                onClick = { if (load.id.isNotBlank()) onLoadClick(load.id) },
                onDelete = { if (load.id.isNotBlank()) onDelete(load.id) },
                rpmThresholds = rpmThresholds,
                modifier = Modifier.weight(1f),
                onCameraClick = {
                    if (load.id.isNotBlank()) {
                        onLoadCamera(load.id, load.tripId, load.date)
                    }
                },
                onScanClick = {
                    if (load.id.isNotBlank()) {
                        onLoadScan(load.id, load.tripId, load.date)
                    }
                },
                settleKey = settleKey,
                enableSwipe = enableSwipe,
            )
        }
        repeat(columns - loads.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
