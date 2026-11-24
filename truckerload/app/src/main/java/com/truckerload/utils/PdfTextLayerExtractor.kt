package com.truckerload.utils

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.max
import kotlin.math.roundToInt

/** Digital PDF text layer (no OCR). Empty/short result means a scanned PDF. */
object PdfTextLayerExtractor {

    fun extract(bytes: ByteArray, maxPages: Int = 12): String {
        if (bytes.isEmpty()) return ""
        return runCatching {
            PDDocument.load(bytes).use { doc ->
                val pages = doc.numberOfPages.coerceAtMost(maxPages)
                buildString {
                    for (pageNumber in 1..pages) {
                        val fallback = runCatching { flatText(doc, pageNumber) }.getOrDefault("")
                        val rows = runCatching { pageRows(doc, pageNumber) }.getOrDefault("")
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
        }.getOrDefault("")
    }

    /** Text in content-stream order, as the PDF draws it. */
    private fun flatText(doc: PDDocument, pageNumber: Int): String {
        val stripper = PDFTextStripper().apply {
            startPage = pageNumber
            endPage = pageNumber
        }
        return stripper.getText(doc).trim()
    }

    /**
     * Groups words that share a baseline into one line, left to right.
     * A flat content-stream read pulls a whole column before the next, so a
     * settlement total can land next to the wrong label.
     */
    private fun pageRows(doc: PDDocument, pageNumber: Int): String {
        val catcher = GlyphCatcher().apply {
            startPage = pageNumber
            endPage = pageNumber
        }
        catcher.getText(doc)
        if (catcher.glyphs.isEmpty()) return ""
        // y grows downward (display space), so ascending = top of page first.
        val buckets = catcher.glyphs.groupBy { (it.y / ROW_BUCKET).roundToInt() }
        return buckets.entries
            .sortedBy { it.key }
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

    /** Collects each word the stripper finds with its baseline start and end. */
    private class GlyphCatcher : PDFTextStripper() {
        val glyphs = ArrayList<Glyph>()

        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
            val positions = textPositions.orEmpty()
            if (positions.isEmpty()) return
            // The stripper glues text drawn out of order into one "word"; split it
            // wherever the baseline changes or the pen jumps back or across a gap.
            var run = ArrayList<TextPosition>()
            for (pos in positions) {
                val last = run.lastOrNull()
                if (last != null && breaksRun(last, pos)) {
                    flush(run)
                    run = ArrayList()
                }
                run += pos
            }
            flush(run)
        }

        private fun breaksRun(last: TextPosition, next: TextPosition): Boolean {
            if (bucket(last.yDirAdj) != bucket(next.yDirAdj)) return true
            val lastEnd = last.xDirAdj + last.widthDirAdj
            return next.xDirAdj < lastEnd - BACKSTEP || next.xDirAdj - lastEnd > WORD_GAP
        }

        private fun bucket(y: Float) = (y / ROW_BUCKET).roundToInt()

        private fun flush(run: List<TextPosition>) {
            if (run.isEmpty()) return
            val text = run.joinToString("") { it.unicode.orEmpty() }
            if (text.isEmpty()) return
            val first = run.first()
            val last = run.last()
            glyphs += Glyph(
                x = first.xDirAdj,
                y = first.yDirAdj,
                endX = last.xDirAdj + last.widthDirAdj,
                text = text,
            )
        }
    }

    private const val ROW_BUCKET = 2.0f
    private const val WORD_GAP = 0.9f
    private const val BACKSTEP = 0.5f
}
