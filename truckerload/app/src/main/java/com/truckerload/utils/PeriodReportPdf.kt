package com.truckerload.utils

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import com.truckerload.domain.report.PeriodReport
import com.truckerload.domain.report.ReportLine
import java.io.ByteArrayOutputStream

/**
 * Midnight & Gold period report.
 * Cream page, midnight header, gold bar on each load table.
 * Row order matches [PeriodReport.plainText].
 */
object PeriodReportPdf {

    private val page = rgb(0xF5, 0xF4, 0xF1)
    private val midnight = rgb(0x1E, 0x21, 0x50)
    private val gold = rgb(0xC8, 0xA8, 0x67)
    private val ink = rgb(0x16, 0x18, 0x2B)
    private val cream = rgb(0xF7, 0xF2, 0xE6)
    private val goldWash = rgb(0xF6, 0xF0, 0xE4)
    private val onGold = rgb(0x1D, 0x17, 0x08)
    private val muted = rgb(0x58, 0x5A, 0x6E)

    fun render(report: PeriodReport, regular: ByteArray, bold: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val pdf = PdfDocument(PdfWriter(out))
        pdf.documentInfo.setTitle("${report.appName} — ${report.title}")
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE) { event ->
            val docEvent = event as PdfDocumentEvent
            val sheet = docEvent.page
            val canvas = PdfCanvas(sheet.newContentStreamBefore(), sheet.resources, docEvent.document)
            val size = sheet.pageSize
            canvas.saveState()
            canvas.setFillColor(page)
            canvas.rectangle(0.0, 0.0, size.width.toDouble(), size.height.toDouble())
            canvas.fill()
            canvas.restoreState()
        }
        val regularFont = font(regular)
        val boldFont = font(bold)
        val doc = Document(pdf, PageSize.A4)
        try {
            doc.setMargins(28f, 32f, 32f, 32f)
            doc.add(headerBand(report, regularFont, boldFont))
            doc.add(sectionTitle(report.summaryTitle, boldFont))
            doc.add(lineTable(report.summary, regularFont, boldFont))
            doc.add(sectionTitle(report.loadsTitle, boldFont))
            val empty = report.emptyLoads
            if (empty != null) {
                doc.add(bodyParagraph(empty, regularFont).setFontColor(muted))
            } else {
                report.loads.forEach { load ->
                    doc.add(loadTable(load.number, load.heading, load.rows, regularFont, boldFont))
                }
            }
            doc.add(
                bodyParagraph(report.footer, regularFont)
                    .setFontSize(8f)
                    .setFontColor(muted)
                    .setMarginTop(14f),
            )
        } finally {
            doc.close()
        }
        return out.toByteArray()
    }

    private fun headerBand(report: PeriodReport, regular: PdfFont, bold: PdfFont): Table {
        val band = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
        val cell = Cell()
            .setBackgroundColor(midnight)
            .setBorder(Border.NO_BORDER)
            .setBorderBottom(SolidBorder(gold, 3f))
            .setPadding(16f)
        cell.add(Paragraph(report.appName).setFont(bold).setFontSize(11f).setFontColor(gold).setMargin(0f))
        cell.add(Paragraph(report.title).setFont(bold).setFontSize(18f).setFontColor(cream).setMarginTop(2f))
        report.header.forEach { line ->
            cell.add(
                Paragraph("${line.label}: ${line.value}")
                    .setFont(regular)
                    .setFontSize(10f)
                    .setFontColor(cream)
                    .setMarginTop(2f),
            )
        }
        band.addCell(cell)
        band.setMarginBottom(8f)
        return band
    }

    private fun sectionTitle(text: String, bold: PdfFont): Paragraph =
        Paragraph(text)
            .setFont(bold)
            .setFontSize(13f)
            .setFontColor(midnight)
            .setMarginTop(14f)
            .setMarginBottom(6f)

    private fun lineTable(rows: List<ReportLine>, regular: PdfFont, bold: PdfFont): Table {
        val table = twoColumnTable()
        rows.forEach { line ->
            table.addCell(labelCell(line.label, bold))
            table.addCell(valueCell(line.value, regular))
        }
        table.setMarginBottom(4f)
        return table
    }

    private fun loadTable(
        number: Int,
        heading: String,
        rows: List<ReportLine>,
        regular: PdfFont,
        bold: PdfFont,
    ): Table {
        val table = twoColumnTable()
        table.addCell(
            Cell(1, 2)
                .add(
                    Paragraph("$number. $heading")
                        .setFont(bold)
                        .setFontSize(11f)
                        .setFontColor(onGold)
                        .setMargin(0f),
                )
                .setBackgroundColor(gold)
                .setBorder(Border.NO_BORDER)
                .setPadding(7f),
        )
        rows.forEach { line ->
            table.addCell(labelCell(line.label, bold))
            table.addCell(valueCell(line.value, regular))
        }
        table.setMarginBottom(12f)
        return table
    }

    private fun twoColumnTable(): Table =
        Table(UnitValue.createPercentArray(floatArrayOf(34f, 66f))).useAllAvailableWidth()

    private fun labelCell(text: String, bold: PdfFont): Cell =
        Cell().add(bodyParagraph(text, bold).setFontSize(9f).setFontColor(muted))
            .setBackgroundColor(goldWash)
            .setBorder(Border.NO_BORDER)
            .setPadding(5f)

    private fun valueCell(text: String, regular: PdfFont): Cell =
        Cell().add(bodyParagraph(text, regular).setFontSize(10f).setFontColor(ink))
            .setBackgroundColor(cream)
            .setBorder(Border.NO_BORDER)
            .setPadding(5f)

    private fun bodyParagraph(text: String, font: PdfFont): Paragraph =
        Paragraph(text).setFont(font).setMargin(0f)

    private fun font(bytes: ByteArray): PdfFont =
        PdfFontFactory.createFont(
            bytes,
            PdfEncodings.IDENTITY_H,
            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED,
        )

    private fun rgb(red: Int, green: Int, blue: Int) = DeviceRgb(red, green, blue)
}
