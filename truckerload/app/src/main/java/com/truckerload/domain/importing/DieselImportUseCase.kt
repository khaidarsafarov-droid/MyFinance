package com.truckerload.domain.importing

import com.truckerload.data.repository.DieselRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.utils.getMillisForWeek
import com.truckerload.utils.getWeekRange
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class DieselImportUseCase @Inject constructor(
    private val dieselRepository: DieselRepository,
) {

    suspend fun buildReview(import: DieselSpreadsheetImport): DieselImportReview {
        val existing = dieselRepository.getDieselForWeekOnce(import.weekNumber, import.year)
        val preview = import.fills.map { fill -> toDiesel(import, fill, id = 0) }
        val comparison = DieselImportComparison(
            existingCount = existing.size,
            importedCount = preview.size,
            existingTotal = existing.sumOf { it.totalAmount },
            importedTotal = preview.sumOf { it.totalAmount },
            existingGallons = existing.sumOf { it.gallons ?: 0.0 },
            importedGallons = preview.sumOf { it.gallons ?: 0.0 },
        )
        return DieselImportReview(
            import = import,
            existing = existing,
            importedPreview = preview,
            comparison = comparison,
            hasConflict = existing.isNotEmpty(),
        )
    }

    suspend fun apply(review: DieselImportReview, action: DieselImportAction) {
        when (action) {
            DieselImportAction.ADD_FROM_FILE -> {
                review.importedPreview.forEach { dieselRepository.insertDiesel(it) }
            }
            DieselImportAction.REPLACE_WEEK -> {
                dieselRepository.replaceWeekFills(
                    weekNumber = review.import.weekNumber,
                    year = review.import.year,
                    fills = review.importedPreview,
                )
            }
        }
    }

    fun toDiesel(import: DieselSpreadsheetImport, fill: ParsedDieselFill, id: Int): Diesel {
        val (weekStart, weekEnd, weekLabel) = getWeekRange(import.weekNumber, import.year, WeekStartRuntime.diesel)
        val addedAt = fill.transactionDate?.let { dateToMillis(it) }
            ?: getMillisForWeek(import.weekNumber, import.year, WeekStartRuntime.diesel)
        return Diesel(
            id = id,
            weekNumber = import.weekNumber,
            year = import.year,
            weekLabel = weekLabel,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            totalAmount = fill.totalAmount,
            gallons = fill.gallons,
            pricePerGallon = fill.pricePerGallon,
            discountPricePerGallon = null,
            location = fill.location,
            rawExtractedText = fill.rawLine,
            sourceFileName = import.fileName,
            addedAt = addedAt,
        )
    }

    private fun dateToMillis(isoDate: String): Long {
        val parts = isoDate.split("-")
        if (parts.size != 3) return System.currentTimeMillis()
        val cal = Calendar.getInstance(Locale.US).apply {
            clear()
            set(Calendar.YEAR, parts[0].toInt())
            set(Calendar.MONTH, parts[1].toInt() - 1)
            set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
