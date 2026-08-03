package com.truckerload.domain.import.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.truckerload.domain.import.ImportTripDedup
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadMessageParser
import com.truckerload.domain.parser.ParseUtils

class HtmlLoadParser(
    private val blockParser: LoadParser = RelayMessageParser(),
) : LoadParser {

    override fun parse(input: String): List<Load> {
        val doc = Ksoup.parse(html = input)
        val loads = mutableListOf<Load>()

        doc.select("table").forEach { table ->
            loads.addAll(parseTable(table, input))
        }

        doc.select("div, li, tr, p").forEach { element ->
            val text = element.text()
            if (text.contains("Trip ID", ignoreCase = true)) {
                blockParser.parse(text).forEach { load ->
                    loads.add(load.copy(rawMessage = input.take(2000)))
                }
            }
        }

        if (loads.isEmpty()) {
            val plain = doc.text()
            if (plain.contains("Trip ID", ignoreCase = true)) {
                loads.addAll(blockParser.parse(plain))
            }
        }

        // FIX: keep latest Trip ID revision — first-wins dropped rate/route updates
        return ImportTripDedup.keepLatestByTripId(loads)
    }

    private fun parseTable(table: Element, rawHtml: String): List<Load> {
        val rows = table.select("tr")
        if (rows.isEmpty()) return emptyList()

        val headers = rows.firstOrNull()?.select("th, td")?.map { it.text().trim() }.orEmpty()
        val tripIdx = headers.indexOfFirst { it.contains("Trip", ignoreCase = true) }
        val rateIdx = headers.indexOfFirst { it.contains("Rate", ignoreCase = true) }
        val milesIdx = headers.indexOfFirst { it.contains("Miles", ignoreCase = true) }

        if (tripIdx == -1 || rateIdx == -1) {
            return rows.mapNotNull { row ->
                val text = row.text()
                if (text.contains("Trip ID", ignoreCase = true)) {
                    blockParser.parse(text).firstOrNull()
                } else null
            }
        }

        return rows.drop(1).mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size <= maxOf(tripIdx, rateIdx)) return@mapNotNull null

            val tripId = cells[tripIdx].text().trim()
            val rate = ParseUtils.parseMoney(cells[rateIdx].text())
            if (rate <= 0) return@mapNotNull null
            val miles = milesIdx.takeIf { it != -1 && it < cells.size }
                ?.let { ParseUtils.parseMiles(cells[it].text()) }
                ?: 0.0

            val blockText = buildString {
                appendLine("Trip ID: $tripId")
                appendLine("Total Rate: $$rate")
                appendLine("Total Loaded Miles: $miles")
                append(row.text())
            }
            blockParser.parse(blockText).firstOrNull()
                ?: LoadMessageParser.parseOne(blockText)?.copy(rawMessage = rawHtml.take(2000))
        }
    }
}
