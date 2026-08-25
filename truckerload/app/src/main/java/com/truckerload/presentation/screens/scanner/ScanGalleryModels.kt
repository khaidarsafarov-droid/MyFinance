package com.truckerload.presentation.screens.scanner

import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.ScanDocumentCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class ScanListRow(
    val scan: ScanEntity,
    val tripId: String,
    val dateLabel: String,
    val routeLabel: String,
) {
    val category: ScanDocumentCategory
        get() = ScanDocumentCategory.fromStored(scan.category)
}

internal fun resolveTripId(scan: ScanEntity, load: Load?): String {
    load?.tripId?.takeIf { it.isNotBlank() }?.let { return it }
    val fromName = scan.fileName.substringBefore('_').substringBefore('.')
    if (fromName.isNotBlank() && fromName != scan.fileName) return fromName
    return scan.fileName
}

internal fun resolveRoute(scan: ScanEntity, load: Load?): String {
    load?.let {
        val route = it.route.ifBlank {
            listOf(it.pointA, it.pointB).filter { p -> p.isNotBlank() }.joinToString(" → ")
        }
        if (route.isNotBlank()) return route
    }
    return extractRouteFromOcr(scan.ocrText) ?: "—"
}

internal fun extractRouteFromOcr(ocr: String): String? {
    if (ocr.isBlank()) return null
    val lines = ocr.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    val shipper = lines.firstOrNull { it.startsWith("Shipper:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
    val consignee = lines.firstOrNull {
        it.startsWith("Consignee:", ignoreCase = true) ||
            it.startsWith("Receiver:", ignoreCase = true) ||
            it.startsWith("Delivery:", ignoreCase = true)
    }?.substringAfter(':')?.trim()
    return when {
        !shipper.isNullOrBlank() && !consignee.isNullOrBlank() -> "$shipper → $consignee"
        !shipper.isNullOrBlank() -> shipper
        else -> null
    }
}

internal fun formatScanDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
}
