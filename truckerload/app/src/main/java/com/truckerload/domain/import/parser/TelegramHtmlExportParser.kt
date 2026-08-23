package com.truckerload.domain.import.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.truckerload.domain.import.ImportTripDedup
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.TelegramMessageDate
import com.truckerload.domain.parser.TelegramStyledTextNormalizer
import java.util.Locale

/** Parses HTML files produced by Telegram Desktop chat export (messages.html, messages2.html, …). */
class TelegramHtmlExportParser(
    private val relayParser: LoadParser = RelayMessageParser(),
) : LoadParser {

    override fun parse(input: String): List<Load> {
        val doc = Ksoup.parse(html = input)
        val loads = mutableListOf<Load>()

        val textNodes = doc.select(".history div.text")
        if (textNodes.isEmpty()) {
            doc.select("div.message div.text").forEach { textDiv ->
                parseTextDiv(textDiv, loads)
            }
        } else {
            textNodes.forEach { textDiv ->
                parseTextDiv(textDiv, loads)
            }
        }

        // FIX: keep latest Trip ID revision — first-wins dropped rate/route updates
        return ImportTripDedup.keepLatestByTripId(loads)
    }

    private fun parseTextDiv(textDiv: Element, loads: MutableList<Load>) {
        val textContent = TelegramStyledTextNormalizer.normalize(extractMessageText(textDiv))
        if (textContent.isBlank()) return

        val messageDiv = textDiv.parents().firstOrNull { el ->
            el.tagName() == "div" && el.classNames().any { it.equals("message", ignoreCase = true) }
        }
        val sender = messageDiv?.select(".from_name")?.text().orEmpty()
        if (isSystemMessage(sender, textContent)) return

        if (!MessageClassifier.isLoadLike(textContent)) {
            return
        }

        // FIX: use Telegram message date so Relay MM/DD anchors to the export year
        val parsedAtMs = extractMessageDateMillis(messageDiv) ?: 0L
        val referenceMillis = parsedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
        relayParser.parse(textContent, referenceMillis).forEach { load ->
            loads.add(
                load.copy(
                    rawMessage = textContent.take(2000),
                    parsedAt = parsedAtMs.takeIf { it > 0L } ?: load.parsedAt,
                )
            )
        }
    }

    /** Telegram Desktop HTML: title="DD.MM.YYYY HH:MM:SS UTC+XX:XX" on .date details. */
    private fun extractMessageDateMillis(messageDiv: Element?): Long? {
        if (messageDiv == null) return null
        val title = messageDiv.select(".date.details, .pull_right.date.details")
            .attr("title")
            .ifBlank { messageDiv.select(".date").attr("title") }
        TelegramMessageDate.parseToMillis(title)?.let { return it }

        val timeText = messageDiv.select(".date.details, .pull_right.date.details, .date")
            .firstOrNull { it.text().isNotBlank() }
            ?.text()
            .orEmpty()
        val dayText = previousServiceDayText(messageDiv)
        return TelegramMessageDate.parseToMillis(
            listOf(dayText, timeText).filter { it.isNotBlank() }.joinToString(" "),
        )
    }

    /** Day-only separators: `<div class="message service"><div class="body details">21 August 2025</div>`. */
    private fun previousServiceDayText(messageDiv: Element): String {
        var sibling = messageDiv.previousElementSibling()
        var hops = 0
        while (sibling != null && hops < 40) {
            val classes = sibling.classNames().map { it.lowercase(Locale.US) }
            if (sibling.tagName() == "div" && classes.any { it == "service" || it == "message" }) {
                val details = sibling.select(".body.details, .details").text().ifBlank { sibling.text() }
                if (TelegramMessageDate.parseToMillis(details) != null) return details
            }
            sibling = sibling.previousElementSibling()
            hops++
        }
        return ""
    }

    private fun extractMessageText(textDiv: Element): String {
        return textDiv.html()
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), "")
            .replace("&nbsp;", " ")
            .replace(Regex("""[ \t]+\n"""), "\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun isSystemMessage(sender: String, text: String): Boolean {
        val systemSenders = listOf("Telegram", "System", "Channel", "Group")
        if (systemSenders.any { sender.contains(it, ignoreCase = true) }) return true
        return text.contains("joined the group", ignoreCase = true) ||
            text.contains("pinned a message", ignoreCase = true) ||
            text.contains("changed the group photo", ignoreCase = true)
    }

    companion object {
        fun isTelegramExport(html: String): Boolean =
            html.contains("page_wrap") &&
                html.contains("history") &&
                html.contains("message default") &&
                html.contains("from_name")
    }
}
