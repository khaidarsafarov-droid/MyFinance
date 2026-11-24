package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadParsingEngine
import com.truckerload.domain.parser.ParseInput

/**
 * Plain text from a file import. Same ranker as chat, Telegram and share
 * ([LoadParsingEngine]), so a pasted copy and an imported file agree.
 */
class TextLoadParser : LoadParser {
    override fun parse(input: String): List<Load> = parse(input, System.currentTimeMillis())

    override fun parse(input: String, referenceMillis: Long): List<Load> =
        LoadParsingEngine.parse(ParseInput.PlainText(input, referenceMillis)).getOrDefault(emptyList())
}
