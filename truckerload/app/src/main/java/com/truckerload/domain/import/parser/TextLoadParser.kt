package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.FlexibleLoadParser
import com.truckerload.domain.parser.LoadMessageParser

/** Plain text: try relay parser, then single-block parse. */
class TextLoadParser : LoadParser {
    override fun parse(input: String): List<Load> = parse(input, System.currentTimeMillis())

    override fun parse(input: String, referenceMillis: Long): List<Load> {
        val all = LoadMessageParser.parseAll(input, referenceMillis)
        if (all.isNotEmpty()) return all
        LoadMessageParser.parseOne(input, referenceMillis)?.let { return listOf(it) }
        return FlexibleLoadParser.parseOne(input, referenceMillis)?.let { listOf(it) } ?: emptyList()
    }
}
