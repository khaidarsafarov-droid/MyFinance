package com.truckerload.domain.ingest

import com.truckerload.domain.importing.DieselSpreadsheetImport
import com.truckerload.domain.importing.DieselSpreadsheetParser

/**
 * One short diesel confirmation for a weekly fleet sheet.
 * Gallon totals (ULSD Qty) are not the dollar amount (ULSD Amt / line totals).
 */
object FleetDieselPreview {

    fun from(
        import: DieselSpreadsheetImport,
        sourceFilePath: String? = null,
        messageDateSeconds: Long? = null,
    ): ReceiptPreview? {
        if (import.fills.isEmpty()) return null
        val total = import.fills.sumOf { it.totalAmount }
        if (total <= 0.0) return null
        val gallons = import.fills.mapNotNull { it.gallons }.sum().takeIf { it > 0.0 }
        return ReceiptPreview(
            kind = ReceiptKind.DIESEL,
            amount = total,
            gallons = gallons,
            pricePerGallon = null,
            date = dateLabel(import),
            location = null,
            vendor = null,
            driverName = import.driverName?.takeIf { it.isNotBlank() },
            tripId = null,
            extractedText = "fleet diesel ${import.fileName}",
            highlightToken = null,
            sourceFileName = import.fileName,
            sourceFilePath = sourceFilePath,
            messageDateSeconds = messageDateSeconds,
        )
    }

    fun dateLabel(import: DieselSpreadsheetImport): String {
        val named = DieselSpreadsheetParser.parseFileName(import.fileName)
        val days = import.fills.mapNotNull { it.transactionDate }.sorted()
        val from = named?.second ?: days.firstOrNull() ?: import.weekStartDate
        val to = named?.third ?: days.lastOrNull() ?: import.weekEndDate
        return if (from == to) from else "$from – $to"
    }
}
