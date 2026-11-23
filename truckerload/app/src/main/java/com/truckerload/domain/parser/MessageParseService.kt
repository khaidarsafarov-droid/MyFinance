package com.truckerload.domain.parser

import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult

/**
 * Central decision service for inbound bot/user text — classifies and parses without external AI.
 */
class MessageParseService {

    fun classify(text: String): MessageType = MessageClassifier.classify(text)

    /** Chat and restore text. Same ranker as files and share — see [LoadParsingEngine]. */
    fun parseLoadsFromMessage(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Result<List<Load>> =
        LoadParsingEngine.parse(ParseInput.PlainText(rawMessage, referenceMillis))

    /**
     * File or share text. Delegates to [LoadParsingEngine] so a PDF and a pasted copy
     * of that PDF cannot take different parsers.
     */
    fun parseLoadsFromInboundText(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Result<List<Load>> = LoadParsingEngine.parse(
        if (fileName.isNullOrBlank()) {
            ParseInput.PlainText(rawMessage, referenceMillis)
        } else {
            ParseInput.ExtractedDocument(rawMessage, fileName, referenceMillis)
        },
    )

    fun parseLoadFromMessage(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Result<Load> = firstLoad(ParseInput.PlainText(rawMessage, referenceMillis))

    /** Add-load screen: the parser whose numbers agree, not the first one that matched. */
    fun parseLoadFromUserInput(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Result<Load> = firstLoad(
        if (fileName.isNullOrBlank()) {
            ParseInput.PlainText(rawMessage, referenceMillis)
        } else {
            ParseInput.ExtractedDocument(rawMessage, fileName, referenceMillis)
        },
    )

    private fun firstLoad(input: ParseInput): Result<Load> =
        LoadParsingEngine.parse(input).mapCatching { loads ->
            loads.firstOrNull() ?: throw IllegalArgumentException("No valid load found in message")
        }

    /**
     * Best-effort fields for the add-load form after paste, file import, or OCR.
     * Always returns a draft (possibly empty) so the UI can show editable boxes.
     */
    fun extractLoadFields(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): LoadDraftFields {
        val input = if (fileName.isNullOrBlank()) {
            ParseInput.PlainText(rawMessage, referenceMillis)
        } else {
            ParseInput.ExtractedDocument(rawMessage, fileName, referenceMillis)
        }
        LoadParsingEngine.rankedLoad(input)?.let { return LoadDraftFields.fromLoad(it) }
        val direct = FlexibleLoadParser.extractFields(rawMessage)
        if (!direct.isEmpty()) return direct
        val flattened = JsonLoadParser.flattenToText(rawMessage)
        return if (flattened.isBlank()) direct else FlexibleLoadParser.extractFields(flattened)
    }

    /** Missing pieces of the best draft this text yields, for confirm-before-save flows. */
    fun completenessOf(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): LoadCompleteness =
        LoadCompletenessChecker.of(extractLoadFields(rawMessage, referenceMillis, fileName))

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
