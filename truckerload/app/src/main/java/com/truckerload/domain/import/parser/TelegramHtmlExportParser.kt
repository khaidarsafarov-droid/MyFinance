package com.truckerload.domain.import.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.TelegramStyledTextNormalizer
import java.util.Locale

/** Parses HTML files produced by Telegram Desktop chat export (messages.html, messages2.html, …). */
class TelegramHtmlExportParser(
    private val relayParser: LoadParser = RelayMessageParser(),
) : LoadParser {

    override fun parse(input: String): List<Load> {
        val doc = Ksoup.parse(html = input)
        // FIX: last-wins map — first-seen distinctBy kept stale Trip ID updates
        val byTrip = LinkedHashMap<String, Load>()

        val textNodes = doc.select(".history div.text")
        if (textNodes.isEmpty()) {
            doc.select("div.message div.text").forEach { textDiv ->
                parseTextDiv(textDiv, byTrip)
            }
        } else {
            textNodes.forEach { textDiv ->
                parseTextDiv(textDiv, byTrip)
            }
        }

        return byTrip.values.toList()
    }

    private fun parseTextDiv(textDiv: Element, byTrip: LinkedHashMap<String, Load>) {
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

        val parsedAtMs = extractMessageDateMillis(messageDiv) ?: 0L
        relayParser.parse(textContent).forEach { load ->
            byTrip[load.tripId.uppercase(Locale.US)] = load.copy(
                rawMessage = textContent.take(2000),
                parsedAt = parsedAtMs,
            )
        }
    }

    /** Telegram Desktop HTML: title="DD.MM.YYYY HH:MM:SS UTC+XX:XX" on .date details. */
    private fun extractMessageDateMillis(messageDiv: Element?): Long? {
        if (messageDiv == null) return null
        val title = messageDiv.select(".date.details, .pull_right.date.details")
            .attr("title")
            .ifBlank { messageDiv.select(".date").attr("title") }
        if (title.isBlank()) return null
        val match = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?""")
            .find(title) ?: return null
        return try {
            val d = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            val y = match.groupValues[3].toInt()
            val h = match.groupValues[4].toInt()
            val min = match.groupValues[5].toInt()
            val s = match.groupValues[6].toIntOrNull() ?: 0
            java.util.Calendar.getInstance(Locale.US).apply {
                set(y, m - 1, d, h, min, s)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (_: Exception) {
            null
        }
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
