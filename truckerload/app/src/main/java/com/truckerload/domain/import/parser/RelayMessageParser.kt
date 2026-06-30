package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadMessageParser

/** Delegates to the production Relay regex parser (multi-PU/DEL). */
class RelayMessageParser : LoadParser {
    override fun parse(input: String): List<Load> =
        LoadMessageParser.parseAll(com.truckerload.domain.parser.TelegramStyledTextNormalizer.normalize(input))
}
