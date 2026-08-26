package com.truckerload.domain.import.parser

import com.truckerload.domain.parser.MessageClassifier

object MessageTypeDetector {

    fun detect(text: String): ImportMessageType {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return ImportMessageType.UNKNOWN

        return when {
            TelegramHtmlExportParser.isTelegramExport(trimmed) -> ImportMessageType.TELEGRAM_HTML
            TelegramJsonExportParser.isTelegramJsonExport(trimmed) -> ImportMessageType.TELEGRAM_JSON
            isCsv(trimmed) -> ImportMessageType.CSV
            isHtml(trimmed) -> ImportMessageType.HTML
            isExportFormat(trimmed) -> ImportMessageType.EXPORT_TEXT
            isRelayFormat(trimmed) -> ImportMessageType.RELAY_TEXT
            MessageClassifier.isLoadLike(trimmed) -> ImportMessageType.RELAY_TEXT
            else -> ImportMessageType.PLAIN_TEXT
        }
    }

    private fun isHtml(text: String): Boolean =
        text.contains("<html", ignoreCase = true) ||
            text.contains("<!DOCTYPE", ignoreCase = true)

    private fun isRelayFormat(text: String): Boolean {
        val relayMarkers = listOf(
            "Trip ID:", "Total Rate:", "Total Loaded Miles:",
            "PU#", "Del-", "Amazon Relay",
        )
        return relayMarkers.count { text.contains(it, ignoreCase = true) } >= 2
    }

    private fun isCsv(text: String): Boolean {
        val firstLine = text.lineSequence().firstOrNull()?.trim().orEmpty()
        if (!firstLine.contains(",")) return false
        val cols = CsvLoadParser.splitCsvLine(firstLine).map { it.trim().lowercase() }
        if (cols.size < 4) return false
        val hasTrip = cols.any { CsvLoadParser.headerHasToken(it, setOf("trip", "tripid")) }
        val hasRate = cols.any { CsvLoadParser.headerHasToken(it, setOf("rate")) }
        return hasTrip && hasRate
    }

    private fun isExportFormat(text: String): Boolean {
        val exportLine = Regex(
            """\d{2}\.\d{2}\.\d{4}\s*\|.+→.+\|.+mi\s*\|""",
            RegexOption.IGNORE_CASE,
        )
        return exportLine.containsMatchIn(text)
    }
}
