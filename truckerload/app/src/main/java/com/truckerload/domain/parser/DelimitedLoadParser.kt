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
        """rate|amount|pay|revenue|gross|price|line\s*-?\s*haul""",
        RegexOption.IGNORE_CASE,
    )

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
                val header = headers[index]
                val value = cells.getOrNull(index).orEmpty()
                if (header.isBlank() || value.isBlank()) null else "$header: $value"
            }
            .joinToString("\n")

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
