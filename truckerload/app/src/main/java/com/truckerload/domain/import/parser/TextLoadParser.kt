package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadMessageParser

/** Plain text: try relay parser, then single-block parse. */
class TextLoadParser : LoadParser {
    override fun parse(input: String): List<Load> {
        val all = LoadMessageParser.parseAll(input)
        if (all.isNotEmpty()) return all
        return LoadMessageParser.parseOne(input)?.let { listOf(it) } ?: emptyList()
    }
}
