package com.truckerload.domain.importing

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Reads the first worksheet of an Office Open XML (.xlsx) workbook into a dense table.
 * No third-party POI dependency — enough for fleet fuel exports and our diesel sheet layout.
 */
object XlsxWorkbookReader {

    fun readAllSheets(bytes: ByteArray): List<List<List<String>>> {
        if (bytes.size < 4 || bytes[0] != 0x50.toByte() || bytes[1] != 0x4B.toByte()) {
            return emptyList()
        }
        val shared = readZipEntry(bytes, "xl/sharedStrings.xml")?.let(::parseSharedStrings).orEmpty()
        val sheetPaths = listZipEntries(bytes)
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sorted()
        return sheetPaths.mapNotNull { path ->
            readZipEntry(bytes, path)?.let { xml -> parseSheet(xml, shared) }
                ?.takeIf { it.isNotEmpty() }
        }
    }

    fun readPrimaryTable(bytes: ByteArray): List<List<String>> =
        readAllSheets(bytes).maxByOrNull { scoreTable(it) }.orEmpty()

    private fun scoreTable(rows: List<List<String>>): Int {
        if (rows.isEmpty()) return 0
        val headerIdx = DieselSpreadsheetParser.findHeaderRowIndex(rows) ?: 0
        val headerScore = rows.getOrNull(headerIdx)?.count { it.isNotBlank() } ?: 0
        return rows.size * 10 + headerScore
    }

    internal fun readZipEntry(bytes: ByteArray, path: String): String? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == path) {
                    return zip.readBytes().decodeToString()
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun listZipEntries(bytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                names += entry.name
                entry = zip.nextEntry
            }
        }
        return names
    }

    internal fun parseSharedStrings(xml: String): List<String> {
        val items = mutableListOf<String>()
        val siRegex = Regex("""<si\b[^>]*>(.*?)</si>""", RegexOption.DOT_MATCHES_ALL)
        val tRegex = Regex("""<t(?:\s[^>]*)?>(.*?)</t>""", RegexOption.DOT_MATCHES_ALL)
        for (match in siRegex.findAll(xml)) {
            val chunk = match.groupValues[1]
            val text = tRegex.findAll(chunk).joinToString("") { unescapeXml(it.groupValues[1]) }
            items += text
        }
        return items
    }

    internal fun parseSheet(xml: String, sharedStrings: List<String>): List<List<String>> {
        val rowRegex = Regex("""<row\b[^>]*>(.*?)</row>""", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("""<c\b([^>]*)>(.*?)</c>""", RegexOption.DOT_MATCHES_ALL)
        val rows = mutableListOf<Pair<Int, MutableMap<Int, String>>>()
        for (rowMatch in rowRegex.findAll(xml)) {
            val rowAttrs = rowMatch.groupValues[0]
            val rowNum = Regex("""r="(\d+)"""").find(rowAttrs)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val cells = mutableMapOf<Int, String>()
            for (cellMatch in cellRegex.findAll(rowMatch.groupValues[1])) {
                val attrs = cellMatch.groupValues[1]
                val inner = cellMatch.groupValues[2]
                val ref = Regex("""r="([A-Za-z]+)(\d+)"""").find(attrs) ?: continue
                val col = columnIndex(ref.groupValues[1])
                val type = Regex("""t="([^"]+)"""").find(attrs)?.groupValues?.get(1)
                val value = Regex("""<v>(.*?)</v>""").find(inner)?.groupValues?.get(1)
                    ?: Regex("""<t(?:\s[^>]*)?>(.*?)</t>""").find(inner)?.groupValues?.get(1)
                if (value.isNullOrBlank()) continue
                val text = when (type) {
                    "s" -> sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                    "inlineStr" -> unescapeXml(value)
                    else -> value
                }.trim()
                if (text.isNotEmpty()) {
                    cells[col] = text
                }
            }
            if (cells.isNotEmpty()) {
                rows += rowNum to cells
            }
        }
        if (rows.isEmpty()) return emptyList()
        val maxCol = rows.maxOf { it.second.keys.maxOrNull() ?: 0 }
        return rows.sortedBy { it.first }.map { (_, cells) ->
            (0..maxCol).map { col -> cells[col].orEmpty() }
        }
    }

    internal fun columnIndex(letters: String): Int {
        var index = 0
        for (ch in letters.uppercase()) {
            index = index * 26 + (ch.code - 'A'.code + 1)
        }
        return index - 1
    }

    private fun unescapeXml(value: String): String =
        value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
}
