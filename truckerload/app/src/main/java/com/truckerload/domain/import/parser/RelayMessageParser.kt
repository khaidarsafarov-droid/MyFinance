package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadParsingEngine
import com.truckerload.domain.parser.ParseInput
import com.truckerload.domain.parser.TelegramStyledTextNormalizer

/**
 * One message from a Telegram chat export. Runs the same ranker as a live Telegram
 * message ([LoadParsingEngine]), so history import and the bot accept the same loads.
 */
class RelayMessageParser : LoadParser {
    override fun parse(input: String): List<Load> = parse(input, System.currentTimeMillis())

    override fun parse(input: String, referenceMillis: Long): List<Load> =
        LoadParsingEngine.parse(
            ParseInput.TelegramMessage(
                text = TelegramStyledTextNormalizer.normalize(input),
                referenceMillis = referenceMillis,
            ),
        ).getOrDefault(emptyList())
}
