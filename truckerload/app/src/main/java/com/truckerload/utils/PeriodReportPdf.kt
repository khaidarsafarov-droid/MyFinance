package com.truckerload.utils

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.truckerload.domain.report.PeriodReport
import com.truckerload.domain.report.PeriodReportPdfLabels
import com.truckerload.domain.report.PeriodReportStatementRow
import com.truckerload.domain.report.englishPdfLabels
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Letter-size load statement. Always English and Locale.US, even when the app
 * language is Russian. White page, grey header, striped rows.
 * The header row repeats when the table splits. No fuel or deduction section.
 */
object PeriodReportPdf {

    private const val FOOTER = "Stored only on this device until you share it."
    private const val EMPTY = "No loads in this period."

    private val ink = Rgb(0x22, 0x22, 0x22)
    private val muted = Rgb(0x55, 0x55, 0x55)
    private val headerBg = Rgb(0xE6, 0xE6, 0xE6)
    private val stripe = Rgb(0xF4, 0xF4, 0xF4)
    private val white = Rgb(0xFF, 0xFF, 0xFF)
    private val line = Rgb(0xBF, 0xBF, 0xBF)
    private val gold = Rgb(0xC8, 0xA8, 0x67)
    private val midnight = Rgb(0x1E, 0x21, 0x50)
    private val columns = floatArrayOf(12f, 15f, 15f, 11f, 11f, 10f, 14f, 12f)

    fun render(report: PeriodReport, regular: ByteArray, bold: ByteArray): ByteArray {
        PDDocument().use { doc ->
            val labels = englishPdfLabels()
            doc.documentInformation.title = labels.title
            val body = PDType0Font.load(doc, ByteArrayInputStream(regular), true)
            val strong = PDType0Font.load(doc, ByteArrayInputStream(bold), true)
            val layout = Layout(doc)
            try {
                letterhead(layout, report, labels, body, strong)
                if (report.statementRows.isEmpty()) {
                    layout.paragraph(EMPTY, body, 11f, muted, marginTop = 16f)
                } else {
                    loadTable(layout, report, labels, body, strong)
                }
                summary(layout, report, labels, body, strong)
                layout.paragraph(FOOTER, body, 8f, muted, marginTop = 16f)
            } finally {
                layout.close()
            }
            val out = ByteArrayOutputStream()
            doc.save(out)
            return out.toByteArray()
        }
    }

    private fun letterhead(
        layout: Layout,
        report: PeriodReport,
        labels: PeriodReportPdfLabels,
        body: PDFont,
        strong: PDFont,
    ) {
        val half = layout.width / 2f
        val left = buildList {
            if (report.companyName.isNotBlank()) add(Block(report.companyName, body, 10f, muted))
            if (report.driverName.isNotBlank()) add(Block(report.driverName, strong, 12f, ink, marginTop = 2f))
        }
        val right = listOf(
            Block(labels.title, strong, 16f, midnight, Align.RIGHT),
            Block("${labels.period}: ${report.statementPeriod}", body, 9f, ink, Align.RIGHT, marginTop = 4f),
            Block("${labels.reportDate}: ${report.statementGenerated}", body, 9f, ink, Align.RIGHT),
        )
        val height = maxOf(layout.stackHeight(left, half), layout.stackHeight(right, half))
        layout.ensureRoom(height + 2f)
        val top = layout.y
        layout.drawStack(left, layout.left, top, half)
        layout.drawStack(right, layout.left + half, top, half)
        layout.y = top - height
        layout.hLine(layout.left, layout.left + layout.width, layout.y - 1f, gold, 2f)
        layout.y -= 2f + 14f
    }

    private fun loadTable(
        layout: Layout,
        report: PeriodReport,
        labels: PeriodReportPdfLabels,
        body: PDFont,
        strong: PDFont,
    ) {
        val widths = columns.map { it / 100f * layout.width }
        val header = listOf(
            labels.loadNumber,
            labels.origin,
            labels.destination,
            labels.shipDate,
            labels.deliveryDate,
            labels.miles,
            labels.gross,
            labels.ratePerMile,
        ).mapIndexed { index, title ->
            Cell(title, strong, if (index >= 5) Align.RIGHT else Align.LEFT, headerBg, line, 0.4f)
        }
        val headerHeight = layout.rowHeight(header, widths, 8f, CELL_PADDING)
        fun drawHeader() = layout.drawRow(header, widths, 8f, CELL_PADDING, headerHeight)

        fun rowFits(height: Float) = layout.y - height >= layout.bottom
        layout.ensureRoom(headerHeight)
        drawHeader()
        report.statementRows.forEachIndexed { index, row ->
            val bg = if (index % 2 == 1) stripe else white
            val cells = bodyCells(row, body, bg)
            val height = layout.rowHeight(cells, widths, 8f, CELL_PADDING)
            if (!rowFits(height)) {
                layout.newPage()
                drawHeader()
            }
            layout.drawRow(cells, widths, 8f, CELL_PADDING, height)
        }
        val totalWidths = listOf(widths.take(5).sum()) + widths.drop(5)
        val totals = listOf(
            Cell(labels.total, strong, Align.RIGHT, null, null, 0f, size = 9f, padding = 5f, topRule = true),
            Cell(report.statementMilesTotal, strong, Align.RIGHT, white, line, 0.3f, topRule = true),
            Cell(report.statementGrossTotal, strong, Align.RIGHT, white, line, 0.3f, topRule = true),
            Cell("", strong, Align.RIGHT, white, line, 0.3f, topRule = true),
        )
        val totalHeight = layout.rowHeight(totals, totalWidths, 8f, CELL_PADDING)
        if (!rowFits(totalHeight)) {
            layout.newPage()
            drawHeader()
        }
        layout.drawRow(totals, totalWidths, 8f, CELL_PADDING, totalHeight)
        layout.y -= 8f
    }

    private fun bodyCells(row: PeriodReportStatementRow, font: PDFont, bg: Rgb): List<Cell> = listOf(
        Cell(row.reference, font, Align.LEFT, bg, line, 0.3f),
        Cell(row.origin, font, Align.LEFT, bg, line, 0.3f),
        Cell(row.destination, font, Align.LEFT, bg, line, 0.3f),
        Cell(row.shipDate, font, Align.LEFT, bg, line, 0.3f),
        Cell(row.deliveryDate, font, Align.LEFT, bg, line, 0.3f),
        Cell(row.miles, font, Align.RIGHT, bg, line, 0.3f),
        Cell(row.gross, font, Align.RIGHT, bg, line, 0.3f),
        Cell(row.ratePerMile, font, Align.RIGHT, bg, line, 0.3f),
    )

    private fun summary(
        layout: Layout,
        report: PeriodReport,
        labels: PeriodReportPdfLabels,
        body: PDFont,
        strong: PDFont,
    ) {
        val nameWidth = layout.width * 0.28f
        val valueWidth = layout.width - nameWidth
        val rows = listOf(
            labels.loadsTotal to report.totals.loadCount.toString(),
            labels.grossTotal to report.statementGrossTotal,
            labels.milesTotal to report.statementMilesTotal.ifBlank { "0" },
            labels.avgRate to report.statementAvgRate.ifBlank { "\u2014" },
        )
        val titleHeight = 12f + layout.textHeight(labels.summary, strong, 13f, layout.width - 4f) + 4f
        val firstRow = rows.first().let { (name, value) -> summaryRowHeight(layout, name, value, body, strong, nameWidth, valueWidth) }
        layout.ensureRoom(titleHeight + firstRow)
        layout.drawText(
            labels.summary,
            strong,
            13f,
            midnight,
            layout.left + 2f,
            layout.y - 12f,
            layout.width - 4f,
            Align.LEFT,
        )
        layout.y -= titleHeight
        rows.forEach { (name, value) ->
            val height = summaryRowHeight(layout, name, value, body, strong, nameWidth, valueWidth)
            layout.ensureRoom(height)
            layout.drawText(name, strong, 10f, ink, layout.left + 2f, layout.y - 2f, nameWidth - 4f, Align.LEFT)
            layout.drawText(value, body, 10f, ink, layout.left + nameWidth + 2f, layout.y - 2f, valueWidth - 4f, Align.LEFT)
            layout.y -= height
        }
    }

    private fun summaryRowHeight(
        layout: Layout,
        name: String,
        value: String,
        body: PDFont,
        strong: PDFont,
        nameWidth: Float,
        valueWidth: Float,
    ): Float = 4f + maxOf(
        layout.textHeight(name, strong, 10f, nameWidth - 4f),
        layout.textHeight(value, body, 10f, valueWidth - 4f),
    )

    private const val CELL_PADDING = 4f

    private enum class Align { LEFT, RIGHT }

    private class Rgb(red: Int, green: Int, blue: Int) {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f
    }

    private class Block(
        val text: String,
        val font: PDFont,
        val size: Float,
        val color: Rgb,
        val align: Align = Align.LEFT,
        val marginTop: Float = 0f,
    )

    private class Cell(
        val text: String,
        val font: PDFont,
        val align: Align,
        val background: Rgb?,
        val border: Rgb?,
        val borderWidth: Float,
        val size: Float? = null,
        val padding: Float? = null,
        val topRule: Boolean = false,
    )

    /** Top-down cursor over Letter pages with 40/36 pt margins. */
    private class Layout(private val doc: PDDocument) {
        private val pageSize = PDRectangle.LETTER
        val left = 36f
        val width = pageSize.width - 36f - 36f
        val bottom = 40f
        private val top = pageSize.height - 40f
        var y = top
        private var stream: PDPageContentStream? = null

        init {
            newPage()
        }

        fun newPage() {
            stream?.close()
            val page = PDPage(pageSize)
            doc.addPage(page)
            stream = PDPageContentStream(doc, page)
            y = top
        }

        fun close() {
            stream?.close()
            stream = null
        }

        fun ensureRoom(height: Float) {
            if (y - height < bottom && y < top) newPage()
        }

        fun paragraph(text: String, font: PDFont, size: Float, color: Rgb, marginTop: Float = 0f) {
            y -= marginTop
            val lines = wrap(text, font, size, width)
            for (line in lines) {
                ensureRoom(lineHeight(size))
                drawLine(line, font, size, color, left, y, width, Align.LEFT)
                y -= lineHeight(size)
            }
        }

        fun stackHeight(blocks: List<Block>, maxWidth: Float): Float =
            blocks.sumOf { (it.marginTop + textHeight(it.text, it.font, it.size, maxWidth)).toDouble() }.toFloat()

        fun drawStack(blocks: List<Block>, x: Float, startY: Float, maxWidth: Float) {
            var cursor = startY
            for (block in blocks) {
                cursor -= block.marginTop
                drawText(block.text, block.font, block.size, block.color, x, cursor, maxWidth, block.align)
                cursor -= textHeight(block.text, block.font, block.size, maxWidth)
            }
        }

        fun textHeight(text: String, font: PDFont, size: Float, maxWidth: Float): Float =
            wrap(text, font, size, maxWidth).size * lineHeight(size)

        /** Draws wrapped text whose first line box starts at [topY]. */
        fun drawText(
            text: String,
            font: PDFont,
            size: Float,
            color: Rgb,
            x: Float,
            topY: Float,
            maxWidth: Float,
            align: Align,
        ) {
            var lineTop = topY
            for (line in wrap(text, font, size, maxWidth)) {
                drawLine(line, font, size, color, x, lineTop, maxWidth, align)
                lineTop -= lineHeight(size)
            }
        }

        fun rowHeight(cells: List<Cell>, widths: List<Float>, size: Float, padding: Float): Float =
            cells.indices.maxOf { i ->
                val cell = cells[i]
                val pad = cell.padding ?: padding
                val fontSize = cell.size ?: size
                textHeight(cell.text, cell.font, fontSize, widths[i] - pad * 2) + pad * 2
            }

        fun drawRow(cells: List<Cell>, widths: List<Float>, size: Float, padding: Float, height: Float) {
            val cs = stream ?: return
            val rowBottom = y - height
            var x = left
            cells.forEachIndexed { i, cell ->
                val w = widths[i]
                cell.background?.let { bg ->
                    cs.setNonStrokingColor(bg.r, bg.g, bg.b)
                    cs.addRect(x, rowBottom, w, height)
                    cs.fill()
                }
                cell.border?.let { border ->
                    cs.setStrokingColor(border.r, border.g, border.b)
                    cs.setLineWidth(cell.borderWidth)
                    cs.addRect(x, rowBottom, w, height)
                    cs.stroke()
                }
                x += w
            }
            x = left
            cells.forEachIndexed { i, cell ->
                if (cell.topRule) hLine(x, x + widths[i], y, ink, 1f)
                x += widths[i]
            }
            x = left
            cells.forEachIndexed { i, cell ->
                val w = widths[i]
                val pad = cell.padding ?: padding
                val fontSize = cell.size ?: size
                val inner = w - pad * 2
                val textH = textHeight(cell.text, cell.font, fontSize, inner)
                val offset = (height - pad * 2 - textH) / 2f
                drawText(cell.text, cell.font, fontSize, ink, x + pad, y - pad - offset, inner, cell.align)
                x += w
            }
            y = rowBottom
        }

        fun hLine(x1: Float, x2: Float, atY: Float, color: Rgb, lineWidth: Float) {
            val cs = stream ?: return
            cs.setStrokingColor(color.r, color.g, color.b)
            cs.setLineWidth(lineWidth)
            cs.moveTo(x1, atY)
            cs.lineTo(x2, atY)
            cs.stroke()
        }

        private fun drawLine(
            text: String,
            font: PDFont,
            size: Float,
            color: Rgb,
            x: Float,
            lineTop: Float,
            maxWidth: Float,
            align: Align,
        ) {
            if (text.isEmpty()) return
            val cs = stream ?: return
            val textWidth = width(text, font, size)
            val startX = if (align == Align.RIGHT) x + maxWidth - textWidth else x
            cs.beginText()
            cs.setFont(font, size)
            cs.setNonStrokingColor(color.r, color.g, color.b)
            cs.newLineAtOffset(startX, lineTop - size * BASELINE)
            cs.showText(text)
            cs.endText()
        }

        private fun lineHeight(size: Float) = size * LEADING

        private fun width(text: String, font: PDFont, size: Float): Float =
            font.getStringWidth(text) / 1000f * size

        /** Splits on spaces; a word wider than the cell breaks by character. */
        fun wrap(raw: String, font: PDFont, size: Float, maxWidth: Float): List<String> {
            val text = printable(raw, font)
            if (text.isEmpty()) return listOf("")
            val lines = ArrayList<String>()
            var current = ""
            for (word in text.split(' ')) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (width(candidate, font, size) <= maxWidth) {
                    current = candidate
                    continue
                }
                if (current.isNotEmpty()) lines += current
                current = ""
                var piece = ""
                for (ch in word) {
                    if (piece.isNotEmpty() && width(piece + ch, font, size) > maxWidth) {
                        lines += piece
                        piece = ""
                    }
                    piece += ch
                }
                current = piece
            }
            lines += current
            return lines
        }

        /** Drops control characters and swaps glyphs the font lacks for '?'. */
        private fun printable(raw: String, font: PDFont): String {
            val sb = StringBuilder()
            var i = 0
            while (i < raw.length) {
                val cp = raw.codePointAt(i)
                val chunk = String(Character.toChars(cp))
                i += chunk.length
                when {
                    cp == '\n'.code || cp == '\r'.code || cp == '\t'.code -> sb.append(' ')
                    Character.isISOControl(cp) -> Unit
                    runCatching { font.encode(chunk) }.isSuccess -> sb.append(chunk)
                    else -> sb.append('?')
                }
            }
            return sb.toString().trim()
        }
    }

    /** iText's default 1.35 leading; first baseline sits about one em below the line top. */
    private const val LEADING = 1.35f
    private const val BASELINE = 1.05f
}
