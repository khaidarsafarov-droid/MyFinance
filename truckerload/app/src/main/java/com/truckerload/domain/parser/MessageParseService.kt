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
     * Any inbound payload of unknown layout: structured JSON, Relay blocks, broker
     * rate confirmations, then loose paste / OCR text. Shared by the bot and the app.
     */
    fun parseLoadsFromInboundText(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Result<List<Load>> = runCatching {
        val json = JsonLoadParser.parseAll(rawMessage, referenceMillis)
        if (json.isNotEmpty()) return@runCatching json
        val relay = LoadMessageParser.parseAll(rawMessage, referenceMillis)
        if (relay.isNotEmpty()) return@runCatching relay
        listOfNotNull(anyLayoutLoad(rawMessage, referenceMillis, fileName))
    }

    /** Non-Relay single load: rate confirmation, loose text, or a flattened JSON dump. */
    private fun anyLayoutLoad(
        rawMessage: String,
        referenceMillis: Long,
        fileName: String? = null,
    ): Load? {
        RateConfirmationLoadParser.parseOne(rawMessage, referenceMillis, fileName)?.let { return it }
        FlexibleLoadParser.parseOne(rawMessage, referenceMillis)?.let { return it }
        val flattened = JsonLoadParser.flattenToText(rawMessage)
        if (flattened.isBlank()) return null
        return FlexibleLoadParser.parseOne(flattened, referenceMillis)
            ?.copy(rawMessage = rawMessage)
    }

    fun parseLoadFromMessage(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Result<Load> = runCatching {
        parseAmazonRelayLoad(rawMessage, referenceMillis)?.load
            ?: throw IllegalArgumentException("No valid load found in message")
    }

    /** Add-load screen: Relay first, then JSON / rate confirmation / loose paste. */
    fun parseLoadFromUserInput(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Result<Load> = runCatching {
        parseAmazonRelayLoad(rawMessage, referenceMillis)?.load
            ?: JsonLoadParser.parseOne(rawMessage, referenceMillis)
            ?: anyLayoutLoad(rawMessage, referenceMillis, fileName)
            ?: throw IllegalArgumentException("No valid load found in message")
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
        parseAmazonRelayLoad(rawMessage, referenceMillis)?.load?.let { return LoadDraftFields.fromLoad(it) }
        JsonLoadParser.parseOne(rawMessage, referenceMillis)?.let { return LoadDraftFields.fromLoad(it) }
        RateConfirmationLoadParser.parseOne(rawMessage, referenceMillis, fileName)?.let {
            return LoadDraftFields.fromLoad(it)
        }
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
