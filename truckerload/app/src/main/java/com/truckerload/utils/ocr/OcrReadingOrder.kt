package com.truckerload.utils.ocr

/**
 * Puts OCR lines in reading order: top to bottom, and left to right on the same row.
 * A photo of a rate confirmation is then one line per printed line, which the parsers expect.
 */
object OcrReadingOrder {

    fun join(lines: List<OcrLine>, rowSlack: Int = 12): String {
        val usable = lines.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return ""
        if (usable.all { it.top == 0 && it.left == 0 }) {
            return usable.joinToString("\n") { it.text.trim() }.trim()
        }
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in usable.sortedBy { it.top }) {
            val row = rows.lastOrNull()
            val anchor = row?.minOf { it.top }
            if (row != null && anchor != null && line.top - anchor <= rowSlack) {
                row.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }
        return rows.joinToString("\n") { row ->
            row.sortedBy { it.left }.joinToString(" ") { it.text.trim() }.trim()
        }.trim()
    }
}

data class OcrLine(
    val top: Int,
    val left: Int,
    val text: String,
)
