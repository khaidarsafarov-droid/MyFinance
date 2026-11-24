package com.truckerload.utils

import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PDFGeneratorTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun merge_keepsEveryPageInSourceOrder() {
        val dir = File(context.cacheDir, "merge-src").apply { mkdirs() }
        val first = File(dir, "a.pdf").also { writePdf(it, "First page A", "Second page A") }
        val second = File(dir, "b.pdf").also { writePdf(it, "Only page B") }

        val merged = PDFGenerator(context).mergePdfFiles(listOf(first, second), "merged.pdf")

        PDDocument.load(merged).use { assertEquals(3, it.numberOfPages) }
        val text = PdfTextLayerExtractor.extract(merged.readBytes())
        assertTrue(text, text.indexOf("First page A") < text.indexOf("Second page A"))
        assertTrue(text, text.indexOf("Second page A") < text.indexOf("Only page B"))
    }

    @Test
    fun merge_singleSourceIsReturnedAsIs() {
        val dir = File(context.cacheDir, "merge-one").apply { mkdirs() }
        val only = File(dir, "only.pdf").also { writePdf(it, "Solo") }
        assertEquals(only, PDFGenerator(context).mergePdfFiles(listOf(only), "unused.pdf"))
    }

    @Test
    fun scannedImages_becomeOneA4PagePerImage() {
        val dir = File(context.cacheDir, "pages").apply { mkdirs() }
        val uris = (1..2).map { index ->
            val file = File(dir, "page$index.jpg")
            val bitmap = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            Uri.fromFile(file)
        }

        val pdf = PDFGenerator(context).createPdfFromPageUris(uris, "scan.pdf")

        PDDocument.load(pdf).use { doc ->
            assertEquals(2, doc.numberOfPages)
            val box = doc.getPage(0).mediaBox
            assertEquals(595f, box.width, 1f)
            assertEquals(842f, box.height, 1f)
        }
    }

    private fun writePdf(file: File, vararg pages: String) {
        PDDocument().use { doc ->
            pages.forEach { text ->
                val page = PDPage()
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.setFont(PDType1Font.HELVETICA, 12f)
                    cs.newLineAtOffset(72f, 700f)
                    cs.showText(text)
                    cs.endText()
                }
            }
            doc.save(file)
        }
    }
}
