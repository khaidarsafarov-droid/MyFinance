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

        return loads.distinctBy { it.tripId.uppercase(Locale.US) }
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

        relayParser.parse(textContent).forEach { load ->
            loads.add(load.copy(rawMessage = textContent.take(2000)))
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
