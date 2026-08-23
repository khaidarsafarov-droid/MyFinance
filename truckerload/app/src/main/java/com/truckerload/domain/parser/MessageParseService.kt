package com.truckerload.domain.parser

import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult

/**
 * Central decision service for inbound bot/user text — classifies and parses without external AI.
 */
class MessageParseService {

    fun classify(text: String): MessageType = MessageClassifier.classify(text)

    fun parseLoadsFromMessage(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Result<List<Load>> = runCatching {
        LoadMessageParser.parseAll(rawMessage, referenceMillis)
    }

    /**
     * Relay blocks first, then broker rate-confirmation PDFs / loose OCR.
     * Used for Telegram files and other inbound documents of unknown layout.
     */
    fun parseLoadsFromInboundText(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Result<List<Load>> = runCatching {
        val relay = LoadMessageParser.parseAll(rawMessage, referenceMillis)
        if (relay.isNotEmpty()) return@runCatching relay
        listOfNotNull(
            RateConfirmationLoadParser.parseOne(rawMessage, referenceMillis, fileName)
                ?: FlexibleLoadParser.parseOne(rawMessage, referenceMillis),
        )
    }

    fun parseLoadFromMessage(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Result<Load> = runCatching {
        parseAmazonRelayLoad(rawMessage, referenceMillis)?.load
            ?: throw IllegalArgumentException("No valid load found in message")
    }

    /** Add-load screen: Relay first, then a looser paste/OCR fallback. */
    fun parseLoadFromUserInput(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Result<Load> = runCatching {
        parseAmazonRelayLoad(rawMessage, referenceMillis)?.load
            ?: RateConfirmationLoadParser.parseOne(rawMessage, referenceMillis)
            ?: FlexibleLoadParser.parseOne(rawMessage, referenceMillis)
            ?: throw IllegalArgumentException("No valid load found in message")
    }

    /**
     * Best-effort fields for the add-load form after paste or OCR.
     * Always returns a draft (possibly empty) so the UI can show editable boxes.
     */
    fun extractLoadFields(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): LoadDraftFields {
        parseAmazonRelayLoad(rawMessage, referenceMillis)?.load?.let { return LoadDraftFields.fromLoad(it) }
        RateConfirmationLoadParser.parseOne(rawMessage, referenceMillis)?.let {
            return LoadDraftFields.fromLoad(it)
        }
        return FlexibleLoadParser.extractFields(rawMessage)
    }

    fun parseAmazonRelayFromMessage(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Result<AmazonRelayParseResult> = runCatching {
        parseAmazonRelayLoad(rawMessage, referenceMillis)
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
