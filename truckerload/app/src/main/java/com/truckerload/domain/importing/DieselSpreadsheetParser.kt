package com.truckerload.domain.importing

import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import java.time.LocalDate
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

    /** Excel 1900 date system: serial 1 is 1900-01-01, so day 0 is 1899-12-30. */
    private val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)
    private const val MIN_EXCEL_SERIAL = 20_000.0
    private const val MAX_EXCEL_SERIAL = 80_000.0

    private val DATE_HEADERS = setOf(
        "date", "transaction date", "trans date", "trx date", "post date", "purchase date",
        "дата",
    )
    private val GALLONS_EXACT = setOf(
        "gallons", "qty", "quantity", "volume", "галлоны", "гал",
    )
    private val GALLONS_HEADERS = GALLONS_EXACT + setOf(
        "fuel qty", "product qty", "total qty",
    )
    private val LIST_PRICE_HEADERS = setOf(
        "ppg", "price per gallon", "unit price", "price/gal", "avg ppg", "price / gal",
        "цена / гал", "цена за галлон",
    )
    private val DISCOUNT_PRICE_HEADERS = setOf(
        "disc ppu", "discount ppu", "disc ppg", "discount ppg", "discount price", "disc price",
    )
    private val AMOUNT_EXACT = setOf(
        "amt", "amount", "net amount", "total amount", "fuel cost", "сумма",
    )
    private val AMOUNT_LOOSE = setOf("gross", "spend", "итого", "total")
    private val LOCATION_HEADERS = setOf(
        "location", "merchant", "site", "station", "merchant name", "stop",
        "место", "локация", "маршрут",
    )
    private val CITY_HEADERS = setOf("city", "город")
    private val STATE_HEADERS = setOf("state", "state/prov", "province", "штат")
    private val PRODUCT_HEADERS = setOf(
        "product", "fuel type", "fuel", "product description", "type", "item",
    )
    private val DRIVER_HEADERS = setOf("driver", "driver name", "driver's name", "водитель")

    fun parse(bytes: ByteArray, fileName: String): DieselSpreadsheetImport {
        val tables = XlsxWorkbookReader.readAllSheets(bytes)
        val table = tables.maxByOrNull { it.size }.orEmpty()
        require(table.isNotEmpty()) { "empty_workbook" }
        val meta = parseFileName(fileName)
        val headerIdx = findHeaderRowIndex(table)
            ?: error("header_not_found")
        val columns = mapColumns(table[headerIdx])
        var driverFromSheet: String? = null
        val fills = table.drop(headerIdx + 1).mapNotNull { row ->
            if (driverFromSheet == null) {
                driverFromSheet = columns.driver?.let { cellAt(row, it) }
            }
            parseDataRow(row, columns)
        }
        require(fills.isNotEmpty()) { "no_fuel_rows" }
        val anchorDate = meta?.second ?: fills.firstNotNullOfOrNull { it.transactionDate }
            ?: error("week_unknown")
        val (weekNumber, year) = getWeekNumberAndYearFromDate(anchorDate, WeekStartRuntime.diesel)
        val (canonicalStart, canonicalEnd, _) = getWeekRange(weekNumber, year, WeekStartRuntime.diesel)
        val weekEnd = fills.mapNotNull { it.transactionDate }.maxOrNull()?.let { lastDate ->
            val (endWeek, endYear) = getWeekNumberAndYearFromDate(lastDate, WeekStartRuntime.diesel)
            getWeekRange(endWeek, endYear, WeekStartRuntime.diesel).second
        } ?: canonicalEnd
        return DieselSpreadsheetImport(
            fileName = fileName,
            driverName = meta?.first ?: driverFromSheet,
            weekStartDate = canonicalStart,
            weekEndDate = maxOf(canonicalEnd, weekEnd),
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
        if (pick(normalized, DATE_HEADERS) != null) score++
        if (pickGallons(normalized) != null) score++
        if (pickAmount(normalized) != null) score++
        if (pick(normalized, LOCATION_HEADERS) != null || pick(normalized, CITY_HEADERS) != null) score++
        if (pickListPrice(normalized) != null || pick(normalized, DISCOUNT_PRICE_HEADERS) != null) score++
        return score
    }

    private data class ColumnMap(
        val date: Int? = null,
        val gallons: Int? = null,
        val listPrice: Int? = null,
        val discountPrice: Int? = null,
        val amount: Int? = null,
        val location: Int? = null,
        val city: Int? = null,
        val state: Int? = null,
        val product: Int? = null,
        val driver: Int? = null,
    )

    private fun mapColumns(header: List<String>): ColumnMap {
        val normalized = header.map { normalizeHeader(it) }
        return ColumnMap(
            date = pick(normalized, DATE_HEADERS),
            gallons = pickGallons(normalized),
            listPrice = pickListPrice(normalized),
            discountPrice = pick(normalized, DISCOUNT_PRICE_HEADERS),
            amount = pickAmount(normalized),
            location = pick(normalized, LOCATION_HEADERS),
            city = pick(normalized, CITY_HEADERS),
            state = pick(normalized, STATE_HEADERS),
            product = pick(normalized, PRODUCT_HEADERS),
            driver = pick(normalized, DRIVER_HEADERS),
        )
    }

    /**
     * Exact paid-amount headers (`Amt`, `Amount`) beat sheet totals such as
     * "Total Payables" and discount columns such as "Disc Amt".
     */
    private fun pickAmount(normalized: List<String>): Int? {
        val exact = normalized.indexOfFirst { it in AMOUNT_EXACT }
        if (exact >= 0) return exact
        return normalized.indexOfFirst { cell ->
            headerHit(cell, AMOUNT_LOOSE) && !isRollupAmount(cell)
        }.takeIf { it >= 0 }
    }

    private fun isRollupAmount(cell: String): Boolean =
        cell.contains("disc") || cell.contains("payable") || cell.contains("fee") ||
            cell.contains("qty") || cell.contains("adjustment") || cell.contains("discount")

    private fun pickGallons(normalized: List<String>): Int? {
        val exact = normalized.indexOfFirst { it in GALLONS_EXACT }
        if (exact >= 0) return exact
        return pick(normalized, GALLONS_HEADERS)
    }

    /** Rack / list price. Discount columns stay on [pick] of [DISCOUNT_PRICE_HEADERS]. */
    private fun pickListPrice(normalized: List<String>): Int? =
        normalized.indexOfFirst { cell ->
            headerHit(cell, LIST_PRICE_HEADERS) && !cell.contains("disc")
        }.takeIf { it >= 0 }

    private fun pick(normalized: List<String>, candidates: Set<String>): Int? =
        normalized.indexOfFirst { cell -> headerHit(cell, candidates) }.takeIf { it >= 0 }

    private fun headerHit(cell: String, candidates: Set<String>): Boolean {
        if (cell.isBlank()) return false
        if (cell in candidates) return true
        val words = cell.split(' ', '/', '-')
        return candidates.any { candidate ->
            if (candidate.length <= 3) words.any { it == candidate } else cell.contains(candidate)
        }
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
        val location = joinLocation(
            name = columns.location?.let { cellAt(row, it) },
            city = columns.city?.let { cellAt(row, it) },
            state = columns.state?.let { cellAt(row, it) },
        )
        val gallons = columns.gallons?.let { parseNumber(row.getOrNull(it)) }
        val listPrice = columns.listPrice?.let { parseNumber(row.getOrNull(it)) }
        val discountPrice = columns.discountPrice?.let { parseNumber(row.getOrNull(it)) }
        val paidPrice = discountPrice ?: listPrice
        var amount = columns.amount?.let { parseNumber(row.getOrNull(it)) }
        if (amount == null && gallons != null && paidPrice != null) {
            amount = gallons * paidPrice
        }
        if (amount == null || amount <= 0.0) return null
        return ParsedDieselFill(
            transactionDate = date,
            location = location,
            gallons = gallons,
            pricePerGallon = listPrice,
            discountPricePerGallon = discountPrice,
            totalAmount = amount,
            productLabel = product,
            rawLine = rawLine,
        )
    }

    internal fun joinLocation(name: String?, city: String?, state: String?): String? {
        val parts = mutableListOf<String>()
        fun add(value: String?) {
            val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return
            if (parts.any { it.equals(trimmed, ignoreCase = true) }) return
            parts += trimmed
        }
        add(name)
        add(city)
        add(state)
        return parts.joinToString(", ").takeIf { it.isNotBlank() }
    }

    private fun cellAt(row: List<String>, index: Int): String? =
        row.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }

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
        return parseExcelSerial(trimmed)
    }

    /** Whole days of an Excel serial. Time-of-day fraction is ignored. */
    internal fun parseExcelSerial(raw: String): String? {
        val serial = raw.toDoubleOrNull() ?: return null
        if (serial < MIN_EXCEL_SERIAL || serial > MAX_EXCEL_SERIAL) return null
        return EXCEL_EPOCH.plusDays(serial.toLong()).toString()
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
