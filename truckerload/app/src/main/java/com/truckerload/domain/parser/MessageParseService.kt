package com.truckerload.domain.parser

import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult

/**
 * Central decision service for inbound bot/user text — classifies and parses without external AI.
 */
class MessageParseService {

    fun classify(text: String): MessageType = MessageClassifier.classify(text)

    fun parseLoadsFromMessage(rawMessage: String): Result<List<Load>> = runCatching {
        LoadMessageParser.parseAll(rawMessage)
    }

    fun parseLoadFromMessage(rawMessage: String): Result<Load> = runCatching {
        parseAmazonRelayLoad(rawMessage)?.load
            ?: throw IllegalArgumentException("No valid load found in message")
    }

    /** Add-load screen: Relay first, then a looser paste/OCR fallback. */
    fun parseLoadFromUserInput(rawMessage: String): Result<Load> = runCatching {
        parseAmazonRelayLoad(rawMessage)?.load
            ?: FlexibleLoadParser.parseOne(rawMessage)
            ?: throw IllegalArgumentException("No valid load found in message")
    }

    fun parseAmazonRelayFromMessage(rawMessage: String): Result<AmazonRelayParseResult> = runCatching {
        parseAmazonRelayLoad(rawMessage)
            ?: throw IllegalArgumentException("No valid Amazon Relay load found in message")
    }

    fun parsePaycheckFromText(text: String): Result<PaycheckParseResult> = runCatching {
        PaycheckTextParser.parse(text)
            ?: throw IllegalArgumentException("No paycheck data found")
    }

    fun parseDieselFromText(text: String): Result<DieselParseResult> = runCatching {
        DieselTextParser.parse(text)
            ?: throw IllegalArgumentException("No diesel data found")
    }
}
