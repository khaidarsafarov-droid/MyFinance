package com.truckerload.utils

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PdfTextLayerExtractorTest {

    @Test
    fun rows_keepLabelAndAmountThatWereDrawnInColumnOrder() {
        val bytes = pdf { canvas ->
            canvas.setTextMatrix(300f, 700f)
            canvas.showText("$7,148.60")
            canvas.setTextMatrix(72f, 700f)
            canvas.showText("Total:")
            canvas.setTextMatrix(280f, 660f)
            canvas.showText("$3,179.87")
            canvas.setTextMatrix(72f, 660f)
            canvas.showText("Grand Total:")
        }

        val text = PdfTextLayerExtractor.extract(bytes)
        assertTrue(text.contains("Total: $7,148.60"))
        assertTrue(text.contains("Grand Total: $3,179.87"))
        assertTrue(text.indexOf("Total:") < text.indexOf("Grand Total:"))
    }

    @Test
    fun blankPage_returnsEmpty() {
        val bytes = pdf { }
        assertEquals("", PdfTextLayerExtractor.extract(bytes))
    }

    private fun pdf(draw: (PdfCanvas) -> Unit): ByteArray {
        val out = ByteArrayOutputStream()
        val writer = PdfWriter(out)
        val doc = PdfDocument(writer)
        val page = doc.addNewPage()
        val canvas = PdfCanvas(page)
        val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
        canvas.beginText()
        canvas.setFontAndSize(font, 12f)
        draw(canvas)
        canvas.endText()
        doc.close()
        return out.toByteArray()
    }
}
