package com.truckerload.domain.importing

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Builds a minimal .xlsx workbook for unit tests. */
object MinimalXlsxBuilder {

    fun build(headers: List<String>, rows: List<List<String>>): ByteArray {
        val table = listOf(headers) + rows
        val strings = linkedSetOf<String>()
        table.flatten().forEach { strings += it }
        val stringList = strings.toList()
        val indexOf: (String) -> Int = { stringList.indexOf(it) }

        val sharedXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${stringList.size}" uniqueCount="${stringList.size}">""")
            stringList.forEach { value ->
                append("<si><t>${escapeXml(value)}</t></si>")
            }
            append("</sst>")
        }

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            table.forEachIndexed { rowIdx, row ->
                val rowNumber = rowIdx + 1
                append("""<row r="$rowNumber">""")
                row.forEachIndexed { colIdx, cell ->
                    val col = columnLetters(colIdx)
                    val ref = "$col$rowNumber"
                    append("""<c r="$ref" t="s"><v>${indexOf(cell)}</v></c>""")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }

        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
            </Types>
        """.trimIndent()

        val workbook = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets><sheet name="Fuel" sheetId="1" r:id="rId1"/></sheets>
            </workbook>
        """.trimIndent()

        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent()

        val rootRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            writeEntry(zip, "[Content_Types].xml", contentTypes)
            writeEntry(zip, "_rels/.rels", rootRels)
            writeEntry(zip, "xl/workbook.xml", workbook)
            writeEntry(zip, "xl/_rels/workbook.xml.rels", rels)
            writeEntry(zip, "xl/sharedStrings.xml", sharedXml)
            writeEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }
        return out.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun columnLetters(index: Int): String {
        var n = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A'.code + (n % 26)).toChar())
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    private fun escapeXml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
