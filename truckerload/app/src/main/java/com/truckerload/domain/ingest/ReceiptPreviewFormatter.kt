package com.truckerload.domain.ingest

import java.util.Locale

object ReceiptPreviewFormatter {

    fun toHtml(preview: ReceiptPreview, guessedLabel: String, askLabel: String): String {
        val lines = mutableListOf<String>()
        lines += "<b>${escape(guessedLabel)}</b>"
        preview.amount?.let {
            lines += "💵 <b>\$%s</b>".format(Locale.US, String.format(Locale.US, "%,.2f", it))
        }
        preview.miles?.let {
            lines += "🛣️ ${String.format(Locale.US, "%,.2f", it)} mi"
        }
        preview.gallons?.let {
            lines += "⛽ ${String.format(Locale.US, "%.2f", it)} gal"
        }
        preview.pricePerGallon?.let {
            lines += "PPG \$%s".format(Locale.US, String.format(Locale.US, "%.3f", it))
        }
        preview.date?.let { lines += "📅 ${escape(it)}" }
        val route = listOfNotNull(preview.pointA, preview.pointB)
            .filter { it.isNotBlank() }
            .joinToString(" → ")
        if (route.isNotBlank()) {
            lines += "📍 ${escape(route)}"
        } else {
            preview.location?.let { lines += "📍 ${escape(it)}" }
        }
        preview.vendor?.let { lines += escape(it) }
        preview.driverName?.let { lines += escape(it) }
        preview.tripId?.let { lines += "Trip ${escape(it)}" }
        lines += ""
        lines += highlightSnippet(preview.extractedText, preview.highlightToken)
        lines += ""
        lines += escape(askLabel)
        val html = lines.joinToString("\n")
        return html.take(3900)
    }

    fun highlightSnippet(text: String, token: String?, maxLen: Int = 900): String {
        val snippet = text.trim().replace("\r\n", "\n")
        val cut = if (snippet.length <= maxLen) snippet else snippet.take(maxLen) + "…"
        val escaped = escape(cut)
        val needle = token?.trim().orEmpty()
        if (needle.length < 2) return "<pre>${escaped.take(maxLen)}</pre>"
        val escapedNeedle = escape(needle)
        val marked = escaped.replaceFirst(escapedNeedle, "<b>$escapedNeedle</b>", ignoreCase = true)
        return marked
    }

    fun escape(raw: String): String =
        raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
