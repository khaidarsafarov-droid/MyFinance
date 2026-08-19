package com.truckerload.utils

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import java.io.ByteArrayInputStream

/** Digital PDF text layer (no OCR). Empty/short result means a scanned PDF. */
object PdfTextLayerExtractor {

    fun extract(bytes: ByteArray, maxPages: Int = 12): String {
        if (bytes.isEmpty()) return ""
        return runCatching {
            PdfReader(ByteArrayInputStream(bytes)).use { reader ->
                PdfDocument(reader).use { doc ->
                    val pages = doc.numberOfPages.coerceAtMost(maxPages)
                    buildString {
                        for (page in 1..pages) {
                            val pageText = PdfTextExtractor.getTextFromPage(doc.getPage(page)).trim()
                            if (pageText.isNotBlank()) {
                                if (isNotEmpty()) append("\n\n")
                                append(pageText)
                            }
                        }
                    }.trim()
                }
            }
        }.getOrDefault("")
    }
}
