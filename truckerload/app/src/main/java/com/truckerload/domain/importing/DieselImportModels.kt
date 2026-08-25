package com.truckerload.domain.importing

import com.truckerload.domain.model.Diesel

/** One fuel row parsed from a spreadsheet before persistence. */
data class ParsedDieselFill(
    val transactionDate: String?,
    val location: String?,
    val gallons: Double?,
    val pricePerGallon: Double?,
    val totalAmount: Double,
    val productLabel: String? = null,
    val rawLine: String = "",
)

/** Parsed spreadsheet ready to import. */
data class DieselSpreadsheetImport(
    val fileName: String,
    val driverName: String?,
    val weekStartDate: String,
    val weekEndDate: String,
    val weekNumber: Int,
    val year: Int,
    val fills: List<ParsedDieselFill>,
)

data class DieselImportComparison(
    val existingCount: Int,
    val importedCount: Int,
    val existingTotal: Double,
    val importedTotal: Double,
    val existingGallons: Double,
    val importedGallons: Double,
) {
    val deltaTotal: Double = importedTotal - existingTotal
    val deltaGallons: Double = importedGallons - existingGallons
}

enum class DieselImportAction {
    ADD_FROM_FILE,
    REPLACE_WEEK,
}

data class DieselImportReview(
    val import: DieselSpreadsheetImport,
    val existing: List<Diesel>,
    val importedPreview: List<Diesel>,
    val comparison: DieselImportComparison,
    val hasConflict: Boolean,
)
