package com.truckerload.domain.importing

import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import java.util.Locale

/**
 * Parses fleet diesel exports such as
 * `Khaidar-Safarov_2026-08-17-2026-08-23.xlsx` (driver + Sun–Sat date range + transaction rows).
 */
object DieselSpreadsheetParser {

    private val FILE_NAME =
        Regex("""(?i)^(.+?)_(\d{4}-\d{2}-\d{2})-(\d{4}-\d{2}-\d{2})(?:\.xlsx)+$""")
    private val ISO_DATE = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    private val US_DATE = Regex("""(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})""")

    private val DATE_HEADERS = setOf(
        "date", "transaction date", "trans date", "trx date", "post date", "purchase date",
        "дата",
    )
    private val GALLONS_HEADERS = setOf(
        "gallons", "qty", "quantity", "volume", "fuel qty", "product qty", "total qty",
        "галлоны", "гал",
    )
    private val PRICE_HEADERS = setOf(
        "ppg", "price per gallon", "unit price", "price/gal", "avg ppg", "price / gal",
        "цена / гал", "цена за галлон",
    )
    private val AMOUNT_HEADERS = setOf(
        "amount", "total", "gross", "net amount", "total amount", "fuel cost", "spend",
        "сумма", "итого",
    )
    private val LOCATION_HEADERS = setOf(
        "location", "merchant", "site", "station", "merchant name", "city", "stop",
        "место", "локация", "маршрут",
    )
    private val PRODUCT_HEADERS = setOf(
        "product", "fuel type", "fuel", "product description", "type",
    )

    fun parse(bytes: ByteArray, fileName: String): DieselSpreadsheetImport {
        val tables = XlsxWorkbookReader.readAllSheets(bytes)
        val table = tables.maxByOrNull { it.size }.orEmpty()
        require(table.isNotEmpty()) { "empty_workbook" }
        val meta = parseFileName(fileName)
        val headerIdx = findHeaderRowIndex(table)
            ?: error("header_not_found")
        val columns = mapColumns(table[headerIdx])
        val fills = table.drop(headerIdx + 1).mapNotNull { row ->
            parseDataRow(row, columns)
        }
        require(fills.isNotEmpty()) { "no_fuel_rows" }
        val weekStart = meta?.second ?: fills.firstNotNullOfOrNull { it.transactionDate }
            ?: error("week_unknown")
        val weekEnd = meta?.third ?: weekStart
        val (weekNumber, year) = getWeekNumberAndYearFromDate(weekStart, WeekStartRuntime.diesel)
        val (canonicalStart, canonicalEnd, _) = getWeekRange(weekNumber, year, WeekStartRuntime.diesel)
        return DieselSpreadsheetImport(
            fileName = fileName,
            driverName = meta?.first,
            weekStartDate = canonicalStart,
            weekEndDate = canonicalEnd,
            weekNumber = weekNumber,
            year = year,
            fills = fills,
        )
    }

    internal fun findHeaderRowIndex(rows: List<List<String>>): Int? {
        val limit = minOf(rows.size, 20)
        var bestIdx: Int? = null
        var bestScore = 0
        for (i in 0 until limit) {
            val score = headerScore(rows[i])
            if (score > bestScore) {
                bestScore = score
                bestIdx = i
            }
        }
        return bestIdx?.takeIf { bestScore >= 2 }
    }

    private fun headerScore(row: List<String>): Int {
        val normalized = row.map { normalizeHeader(it) }
        var score = 0
        if (normalized.any { DATE_HEADERS.any { h -> it.contains(h) } }) score++
        if (normalized.any { GALLONS_HEADERS.any { h -> it.contains(h) } }) score++
        if (normalized.any { AMOUNT_HEADERS.any { h -> it.contains(h) } }) score++
        if (normalized.any { LOCATION_HEADERS.any { h -> it.contains(h) } }) score++
        if (normalized.any { PRICE_HEADERS.any { h -> it.contains(h) } }) score++
        return score
    }

    private data class ColumnMap(
        val date: Int? = null,
        val gallons: Int? = null,
        val price: Int? = null,
        val amount: Int? = null,
        val location: Int? = null,
        val product: Int? = null,
    )

    private fun mapColumns(header: List<String>): ColumnMap {
        val normalized = header.map { normalizeHeader(it) }
        fun find(candidates: Set<String>): Int? =
            normalized.indexOfFirst { cell -> candidates.any { cell.contains(it) } }.takeIf { it >= 0 }
        return ColumnMap(
            date = find(DATE_HEADERS),
            gallons = find(GALLONS_HEADERS),
            price = find(PRICE_HEADERS),
            amount = find(AMOUNT_HEADERS),
            location = find(LOCATION_HEADERS),
            product = find(PRODUCT_HEADERS),
        )
    }

    private fun parseDataRow(row: List<String>, columns: ColumnMap): ParsedDieselFill? {
        val rawLine = row.filter { it.isNotBlank() }.joinToString(" | ")
        if (rawLine.isBlank()) return null
        val lower = rawLine.lowercase(Locale.US)
        if (lower.contains("total") && !lower.contains("total qty")) return null
        if (lower == "итого" || lower.startsWith("total,")) return null

        val product = columns.product?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotBlank() }
        if (product != null && !isDieselProduct(product)) return null

        val date = columns.date?.let { parseDateCell(row.getOrNull(it)) }
        val location = columns.location?.let { row.getOrNull(it)?.trim()?.takeIf { s -> s.isNotBlank() } }
        val gallons = columns.gallons?.let { parseNumber(row.getOrNull(it)) }
        val price = columns.price?.let { parseNumber(row.getOrNull(it)) }
        var amount = columns.amount?.let { parseNumber(row.getOrNull(it)) }
        if (amount == null && gallons != null && price != null) {
            amount = gallons * price
        }
        if (amount == null || amount <= 0.0) return null
        return ParsedDieselFill(
            transactionDate = date,
            location = location,
            gallons = gallons,
            pricePerGallon = price,
            totalAmount = amount,
            productLabel = product,
            rawLine = rawLine,
        )
    }

    private fun isDieselProduct(label: String): Boolean {
        val lower = label.lowercase(Locale.US)
        if (lower.contains("def") || lower.contains("reefer") || lower.contains("unleaded") ||
            lower.contains("gasoline") || lower.contains("cash advance")
        ) {
            return false
        }
        return lower.contains("diesel") || lower.contains("d2") || lower.contains("ulsd") ||
            lower.contains("fuel") || lower.isBlank()
    }

    internal fun parseFileName(fileName: String): Triple<String, String, String>? {
        val base = fileName.substringAfterLast('/').substringAfterLast('\\')
        val match = FILE_NAME.matchEntire(base) ?: return null
        val driver = match.groupValues[1].replace('-', ' ').trim()
        return Triple(driver, match.groupValues[2], match.groupValues[3])
    }

    internal fun normalizeHeader(value: String): String =
        value.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")

    internal fun parseDateCell(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        ISO_DATE.find(trimmed)?.let {
            return String.format(Locale.US, "%04d-%02d-%02d", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }
        US_DATE.find(trimmed)?.let {
            var year = it.groupValues[3].toInt()
            if (year < 100) year += 2000
            val a = it.groupValues[1].toInt()
            val b = it.groupValues[2].toInt()
            val (month, day) = if (a > 12) b to a else a to b
            return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        }
        return null
    }

    internal fun parseNumber(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw
            .replace("$", "")
            .replace("USD", "", ignoreCase = true)
            .replace("\u00a0", "")
            .replace(" ", "")
            .replace(",", "")
            .trim()
        return cleaned.toDoubleOrNull()
    }
}
