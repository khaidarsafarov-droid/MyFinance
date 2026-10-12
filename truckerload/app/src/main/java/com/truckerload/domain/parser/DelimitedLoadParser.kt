package com.truckerload.domain.parser

import com.truckerload.domain.import.parser.CsvLoadParser
import com.truckerload.domain.model.Load

/**
 * Header-based CSV / TSV / semicolon exports.
 *
 * Each row is rewritten as `header: value` lines and handed to [FlexibleLoadParser],
 * so any column naming that parser already understands works here too.
 */
object DelimitedLoadParser {

    private val delimiters = charArrayOf(',', '\t', ';')
    private val rateHeader = Regex(
        """rate|amount|pay|revenue|gross|price|line\s*-?\s*haul|linehaul""",
        RegexOption.IGNORE_CASE,
    )
    private val milesHeader = Regex(
        """mile|mileage|distance|loaded""",
        RegexOption.IGNORE_CASE,
    )
    private val pickupHeader = Regex(
        """origin|shipper|pickup|pick\s*up|from|pu\b""",
        RegexOption.IGNORE_CASE,
    )
    private val deliveryHeader = Regex(
        """destination|consignee|delivery|deliver|to\b|del\b""",
        RegexOption.IGNORE_CASE,
    )
    private val tripHeader = Regex(
        """trip|load\s*(?:#|id|number)|confirmation|pro\b|bol\b""",
        RegexOption.IGNORE_CASE,
    )
    private val dateHeader = Regex("""\bdate\b|pickup\s*date""", RegexOption.IGNORE_CASE)
    private val weightOrGallons = Regex("""weight|gallon|gal\b|fuel""", RegexOption.IGNORE_CASE)

    fun parseAll(raw: String, referenceMillis: Long = System.currentTimeMillis()): List<Load> {
        val lines = raw.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return emptyList()

        val delimiter = bestDelimiter(lines) ?: return emptyList()
        val headers = split(lines.first(), delimiter)
        if (headers.none { rateHeader.containsMatchIn(it) }) return emptyList()

        return lines.drop(1).mapNotNull { line ->
            val cells = split(line, delimiter)
            if (cells.size < MIN_COLUMNS) return@mapNotNull null
            FlexibleLoadParser.parseOne(labelledBlock(headers, cells), referenceMillis)
                ?.copy(rawMessage = line)
        }
    }

    fun parseOne(raw: String, referenceMillis: Long = System.currentTimeMillis()): Load? =
        parseAll(raw, referenceMillis).firstOrNull()

    private fun labelledBlock(headers: List<String>, cells: List<String>): String =
        headers.indices
            .mapNotNull { index ->
                val header = canonicalHeader(headers[index])
                val value = cells.getOrNull(index).orEmpty()
                if (header.isBlank() || value.isBlank()) null else "$header: $value"
            }
            .joinToString("\n")

    /**
     * Spreadsheet headers rarely say "Total Rate". Map the ones drivers actually export
     * onto the labels [FlexibleLoadParser] already understands.
     */
    private fun canonicalHeader(header: String): String {
        val trimmed = header.trim()
        if (trimmed.isBlank() || weightOrGallons.containsMatchIn(trimmed)) return trimmed
        return when {
            dateHeader.containsMatchIn(trimmed) -> "Date"
            tripHeader.containsMatchIn(trimmed) -> "Trip ID"
            milesHeader.containsMatchIn(trimmed) -> "Total Loaded Miles"
            rateHeader.containsMatchIn(trimmed) -> "Total Rate"
            pickupHeader.containsMatchIn(trimmed) -> "Pickup"
            deliveryHeader.containsMatchIn(trimmed) -> "Delivery"
            else -> trimmed
        }
    }

    /** The delimiter whose column count repeats on the header and at least one row. */
    private fun bestDelimiter(lines: List<String>): Char? = delimiters
        .filter { candidate ->
            val headerCount = split(lines.first(), candidate).size
            headerCount >= MIN_COLUMNS &&
                lines.drop(1).any { split(it, candidate).size == headerCount }
        }
        .maxByOrNull { split(lines.first(), it).size }

    private fun split(line: String, delimiter: Char): List<String> = when (delimiter) {
        ',' -> CsvLoadParser.splitCsvLine(line)
        else -> line.split(delimiter).map { it.trim().trim('"').trim() }
    }

    private const val MIN_COLUMNS = 3
}
