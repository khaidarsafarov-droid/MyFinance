package com.truckerload.domain.ingest

import com.truckerload.domain.importing.DieselSpreadsheetImport
import com.truckerload.domain.importing.DieselSpreadsheetParser
import com.truckerload.domain.money.Money

/**
 * One short diesel confirmation for a weekly fleet sheet.
 * Gallon totals stay gallons. The dollar total is the sum of fuel-row amounts in cents.
 */
object FleetDieselPreview {

    fun from(
        import: DieselSpreadsheetImport,
        sourceFilePath: String? = null,
        messageDateSeconds: Long? = null,
    ): ReceiptPreview? {
        if (import.fills.isEmpty()) return null
        val total = Money.fromDollars(import.fills.sumOf { it.totalAmount })
        if (!total.isPositive()) return null
        val gallons = import.fills.mapNotNull { it.gallons }.sum().takeIf { it > 0.0 }
        return ReceiptPreview(
            kind = ReceiptKind.DIESEL,
            amount = total.cents / 100.0,
            gallons = gallons,
            pricePerGallon = null,
            date = dateLabel(import),
            location = null,
            vendor = null,
            driverName = import.driverName?.takeIf { it.isNotBlank() },
            tripId = null,
            extractedText = "",
            highlightToken = null,
            sourceFileName = import.fileName,
            sourceFilePath = sourceFilePath,
            messageDateSeconds = messageDateSeconds,
            includeStatement = false,
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
