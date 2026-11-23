package com.truckerload.utils

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.truckerload.domain.report.PeriodReport
import com.truckerload.domain.report.PeriodReportPdfLabels
import com.truckerload.domain.report.PeriodReportStatementRow
import com.truckerload.domain.report.englishPdfLabels
import java.io.ByteArrayOutputStream

/**
 * Letter-size load statement. Always English and Locale.US, even when the app
 * language is Russian. White page, grey header, striped rows.
 * The header row repeats when the table splits. No fuel or deduction section.
 */
object PeriodReportPdf {

    private const val FOOTER = "Stored only on this device until you share it."
    private const val EMPTY = "No loads in this period."

    private val ink = rgb(0x22, 0x22, 0x22)
    private val muted = rgb(0x55, 0x55, 0x55)
    private val headerBg = rgb(0xE6, 0xE6, 0xE6)
    private val stripe = rgb(0xF4, 0xF4, 0xF4)
    private val line = rgb(0xBF, 0xBF, 0xBF)
    private val gold = rgb(0xC8, 0xA8, 0x67)
    private val midnight = rgb(0x1E, 0x21, 0x50)
    private val columns = floatArrayOf(12f, 15f, 15f, 11f, 11f, 10f, 14f, 12f)

    fun render(report: PeriodReport, regular: ByteArray, bold: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val pdf = PdfDocument(PdfWriter(out))
        val labels = englishPdfLabels()
        pdf.documentInfo.setTitle(labels.title)
        val body = font(regular)
        val strong = font(bold)
        val doc = Document(pdf, PageSize.LETTER)
        try {
            doc.setMargins(40f, 36f, 40f, 36f)
            doc.add(letterhead(report, labels, body, strong))
            if (report.statementRows.isEmpty()) {
                doc.add(paragraph(EMPTY, body, 11f).setFontColor(muted).setMarginTop(16f))
            } else {
                doc.add(loadTable(report, labels, body, strong))
            }
            doc.add(summary(report, labels, body, strong))
            doc.add(paragraph(FOOTER, body, 8f).setFontColor(muted).setMarginTop(16f))
        } finally {
            doc.close()
        }
        return out.toByteArray()
    }

    private fun letterhead(
        report: PeriodReport,
        labels: PeriodReportPdfLabels,
        body: PdfFont,
        strong: PdfFont,
    ): Table {
        val band = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
        val left = Cell().setBorder(Border.NO_BORDER).setPadding(0f)
        if (report.companyName.isNotBlank()) {
            left.add(paragraph(report.companyName, body, 10f).setFontColor(muted))
        }
        if (report.driverName.isNotBlank()) {
            left.add(paragraph(report.driverName, strong, 12f).setMarginTop(2f))
        }
        band.addCell(left)
        val right = Cell().setBorder(Border.NO_BORDER).setPadding(0f)
        right.add(paragraph(labels.title, strong, 16f).setFontColor(midnight).setTextAlignment(TextAlignment.RIGHT))
        val period = report.statementPeriod
        val generated = report.statementGenerated
        right.add(paragraph("${labels.period}: $period", body, 9f).setTextAlignment(TextAlignment.RIGHT).setMarginTop(4f))
        right.add(paragraph("${labels.reportDate}: $generated", body, 9f).setTextAlignment(TextAlignment.RIGHT))
        band.addCell(right)
        band.setBorderBottom(SolidBorder(gold, 2f))
        band.setMarginBottom(14f)
        return band
    }

    private fun loadTable(
        report: PeriodReport,
        labels: PeriodReportPdfLabels,
        body: PdfFont,
        strong: PdfFont,
    ): Table {
        val table = Table(UnitValue.createPercentArray(columns)).useAllAvailableWidth()
        listOf(
            labels.loadNumber,
            labels.origin,
            labels.destination,
            labels.shipDate,
            labels.deliveryDate,
            labels.miles,
            labels.gross,
            labels.ratePerMile,
        ).forEachIndexed { index, title ->
            val align = if (index >= 5) TextAlignment.RIGHT else TextAlignment.LEFT
            table.addHeaderCell(head(title, strong, align))
        }
        report.statementRows.forEachIndexed { index, row ->
            val bg = if (index % 2 == 1) stripe else DeviceRgb(255, 255, 255)
            bodyRow(table, row, body, bg)
        }
        totalRow(table, report, labels, strong)
        table.setMarginBottom(8f)
        return table
    }

    private fun bodyRow(table: Table, row: PeriodReportStatementRow, font: PdfFont, bg: DeviceRgb) {
        table.addCell(data(row.reference, font, TextAlignment.LEFT, bg))
        table.addCell(data(row.origin, font, TextAlignment.LEFT, bg))
        table.addCell(data(row.destination, font, TextAlignment.LEFT, bg))
        table.addCell(data(row.shipDate, font, TextAlignment.LEFT, bg))
        table.addCell(data(row.deliveryDate, font, TextAlignment.LEFT, bg))
        table.addCell(data(row.miles, font, TextAlignment.RIGHT, bg))
        table.addCell(data(row.gross, font, TextAlignment.RIGHT, bg))
        table.addCell(data(row.ratePerMile, font, TextAlignment.RIGHT, bg))
    }

    private fun totalRow(
        table: Table,
        report: PeriodReport,
        labels: PeriodReportPdfLabels,
        strong: PdfFont,
    ) {
        val label = Cell(1, 5)
            .add(paragraph(labels.total, strong, 9f).setTextAlignment(TextAlignment.RIGHT))
            .setBorder(Border.NO_BORDER)
            .setBorderTop(SolidBorder(ink, 1f))
            .setPadding(5f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
        table.addCell(label)
        table.addCell(data(report.statementMilesTotal, strong, TextAlignment.RIGHT, DeviceRgb(255, 255, 255), top = true))
        table.addCell(data(report.statementGrossTotal, strong, TextAlignment.RIGHT, DeviceRgb(255, 255, 255), top = true))
        table.addCell(data("", strong, TextAlignment.RIGHT, DeviceRgb(255, 255, 255), top = true))
    }

    private fun summary(
        report: PeriodReport,
        labels: PeriodReportPdfLabels,
        body: PdfFont,
        strong: PdfFont,
    ): Table {
        val block = Table(UnitValue.createPercentArray(floatArrayOf(28f, 72f))).useAllAvailableWidth()
        block.addCell(
            Cell(1, 2).add(paragraph(labels.summary, strong, 13f).setFontColor(midnight))
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(12f)
                .setPaddingBottom(4f),
        )
        listOf(
            labels.loadsTotal to report.totals.loadCount.toString(),
            labels.grossTotal to report.statementGrossTotal,
            labels.milesTotal to report.statementMilesTotal.ifBlank { "0" },
            labels.avgRate to report.statementAvgRate.ifBlank { "\u2014" },
        ).forEach { (name, value) ->
            block.addCell(plain(name, strong))
            block.addCell(plain(value, body))
        }
        return block
    }

    private fun head(text: String, font: PdfFont, align: TextAlignment): Cell =
        Cell().add(paragraph(text, font, 8f).setTextAlignment(align))
            .setBackgroundColor(headerBg)
            .setBorder(SolidBorder(line, 0.4f))
            .setPadding(4f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)

    private fun data(
        text: String,
        font: PdfFont,
        align: TextAlignment,
        bg: DeviceRgb,
        top: Boolean = false,
    ): Cell {
        val cell = Cell().add(paragraph(text, font, 8f).setTextAlignment(align))
            .setBackgroundColor(bg)
            .setBorder(SolidBorder(line, 0.3f))
            .setPadding(4f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
        if (top) cell.setBorderTop(SolidBorder(ink, 1f))
        return cell
    }

    private fun plain(text: String, font: PdfFont): Cell =
        Cell().add(paragraph(text, font, 10f))
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(2f)
            .setPaddingBottom(2f)

    private fun paragraph(text: String, font: PdfFont, size: Float): Paragraph =
        Paragraph(text).setFont(font).setFontSize(size).setFontColor(ink).setMargin(0f)

    private fun font(bytes: ByteArray): PdfFont =
        PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)

    private fun rgb(red: Int, green: Int, blue: Int) = DeviceRgb(red, green, blue)
}
