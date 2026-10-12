package com.truckerload.utils

import com.itextpdf.kernel.geom.Vector
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener
import java.io.ByteArrayInputStream
import java.util.EnumSet
import kotlin.math.max
import kotlin.math.roundToInt

/** Digital PDF text layer (no OCR). Empty/short result means a scanned PDF. */
object PdfTextLayerExtractor {

    fun extract(bytes: ByteArray, maxPages: Int = 12): String {
        if (bytes.isEmpty()) return ""
        return runCatching {
            PdfReader(ByteArrayInputStream(bytes)).use { reader ->
                PdfDocument(reader).use { doc ->
                    val pages = doc.numberOfPages.coerceAtMost(maxPages)
                    buildString {
                        for (pageNumber in 1..pages) {
                            val page = doc.getPage(pageNumber)
                            val fallback = runCatching {
                                PdfTextExtractor.getTextFromPage(page).trim()
                            }.getOrDefault("")
                            val rows = runCatching { pageRows(page) }.getOrDefault("")
                            val pageText = when {
                                rows.isBlank() -> fallback
                                fallback.length > rows.length * 2 &&
                                    fallback.length - rows.length > 80 -> fallback
                                else -> rows
                            }
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

    /**
     * Groups glyphs that share a baseline into one line, left to right.
     * A flat content-stream read pulls a whole column before the next, so a
     * settlement total can land next to the wrong label.
     */
    private fun pageRows(page: com.itextpdf.kernel.pdf.PdfPage): String {
        val catcher = GlyphCatcher()
        PdfCanvasProcessor(catcher).processPageContent(page)
        if (catcher.glyphs.isEmpty()) return ""
        val buckets = catcher.glyphs.groupBy { (it.y / ROW_BUCKET).roundToInt() }
        return buckets.entries
            .sortedByDescending { it.key }
            .joinToString("\n") { (_, row) -> renderLine(row) }
            .trim()
    }

    private fun renderLine(glyphs: List<Glyph>): String {
        val ordered = glyphs.sortedBy { it.x }
        val sb = StringBuilder()
        var prevEnd = Float.NEGATIVE_INFINITY
        for (glyph in ordered) {
            val gap = glyph.x - prevEnd
            if (sb.isNotEmpty() && gap > WORD_GAP && !glyph.text.startsWith(" ")) {
                sb.append(' ')
            }
            sb.append(glyph.text)
            prevEnd = max(prevEnd, glyph.endX)
        }
        return sb.toString().replace(Regex("""[ \t]{2,}"""), " ").trim()
    }

    private data class Glyph(val x: Float, val y: Float, val endX: Float, val text: String)

    private class GlyphCatcher : IEventListener {
        val glyphs = ArrayList<Glyph>()

        override fun eventOccurred(data: IEventData?, type: EventType?) {
            if (type != EventType.RENDER_TEXT || data !is TextRenderInfo) return
            val text = data.text ?: return
            if (text.isEmpty()) return
            val start = data.baseline.startPoint
            val end = data.baseline.endPoint
            glyphs += Glyph(
                x = start.get(Vector.I1),
                y = start.get(Vector.I2),
                endX = end.get(Vector.I1),
                text = text,
            )
        }

        override fun getSupportedEvents(): Set<EventType> =
            EnumSet.of(EventType.RENDER_TEXT)
    }

    private const val ROW_BUCKET = 2.0f
    private const val WORD_GAP = 0.9f
}
