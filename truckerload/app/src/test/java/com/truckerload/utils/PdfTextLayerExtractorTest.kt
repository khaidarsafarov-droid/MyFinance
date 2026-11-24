package com.truckerload.utils

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

/** PdfBox-Android logs through android.util.Log and reads assets, so it runs on Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfTextLayerExtractorTest {

    @Before
    fun setUp() {
        PdfBox.init(RuntimeEnvironment.getApplication())
    }


    @Test
    fun rows_keepLabelAndAmountThatWereDrawnInColumnOrder() {
        val bytes = pdf { canvas ->
            canvas.at(300f, 700f, "$7,148.60")
            canvas.at(72f, 700f, "Total:")
            canvas.at(280f, 660f, "$3,179.87")
            canvas.at(72f, 660f, "Grand Total:")
        }

        val text = PdfTextLayerExtractor.extract(bytes)
        assertTrue(text, text.contains("Total: $7,148.60"))
        assertTrue(text, text.contains("Grand Total: $3,179.87"))
        assertTrue(text.indexOf("Total:") < text.indexOf("Grand Total:"))
    }

    @Test
    fun wordsOnOneBaseline_stayInReadingOrder() {
        val bytes = pdf { canvas ->
            canvas.at(400f, 500f, "$1,250.00")
            canvas.at(72f, 500f, "Linehaul")
            canvas.at(200f, 500f, "Load 4471")
        }

        val text = PdfTextLayerExtractor.extract(bytes)
        assertEquals("Linehaul Load 4471 $1,250.00", text)
    }

    @Test
    fun blankPage_returnsEmpty() {
        val bytes = pdf { }
        assertEquals("", PdfTextLayerExtractor.extract(bytes))
    }

    @Test
    fun notAPdf_returnsEmpty() {
        assertEquals("", PdfTextLayerExtractor.extract("hello".toByteArray()))
        assertEquals("", PdfTextLayerExtractor.extract(ByteArray(0)))
    }

    private fun PDPageContentStream.at(x: Float, y: Float, text: String) {
        beginText()
        setFont(PDType1Font.HELVETICA, 12f)
        newLineAtOffset(x, y)
        showText(text)
        endText()
    }

    private fun pdf(draw: (PDPageContentStream) -> Unit): ByteArray {
        PDDocument().use { doc ->
            val page = PDPage()
            doc.addPage(page)
            PDPageContentStream(doc, page).use(draw)
            val out = ByteArrayOutputStream()
            doc.save(out)
            return out.toByteArray()
        }
    }
}
