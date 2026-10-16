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
     * Any inbound payload of unknown layout. Every parser runs; the ranker keeps the
     * candidate whose rate, miles, and stops agree. Shared by the bot and the app.
     */
    fun parseLoadsFromInboundText(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Result<List<Load>> = runCatching {
        val chosen = LoadCandidateRanker.choose(loadGroups(rawMessage, referenceMillis, fileName))
        if (LoadCandidateRanker.keepDespiteReceipt(classify(rawMessage), chosen)) chosen else emptyList()
    }

    /** Every parser that can read this text. The ranker, not call order, decides the winner. */
    private fun loadGroups(
        rawMessage: String,
        referenceMillis: Long,
        fileName: String?,
    ): List<LoadParseGroup> {
        val flattened = JsonLoadParser.flattenToText(rawMessage)
        val fromFlat = if (flattened.isBlank()) {
            emptyList()
        } else {
            listOfNotNull(
                FlexibleLoadParser.parseOne(flattened, referenceMillis)?.copy(rawMessage = rawMessage),
            )
        }
        return listOf(
            LoadParseGroup(LoadParseSource.JSON, JsonLoadParser.parseAll(rawMessage, referenceMillis)),
            LoadParseGroup(LoadParseSource.RELAY, LoadMessageParser.parseAll(rawMessage, referenceMillis)),
            LoadParseGroup(LoadParseSource.DELIMITED, DelimitedLoadParser.parseAll(rawMessage, referenceMillis)),
            LoadParseGroup(
                LoadParseSource.TEMPLATE,
                listOfNotNull(BrokerRateTemplateEngine.parseOne(rawMessage, referenceMillis, fileName)),
            ),
            LoadParseGroup(
                LoadParseSource.RATE_CONFIRMATION,
                listOfNotNull(RateConfirmationLoadParser.parseOne(rawMessage, referenceMillis, fileName)),
            ),
            LoadParseGroup(
                LoadParseSource.FLEXIBLE,
                listOfNotNull(FlexibleLoadParser.parseOne(rawMessage, referenceMillis)),
            ),
            LoadParseGroup(LoadParseSource.FLEXIBLE, fromFlat),
        )
    }

    private fun bestLoad(
        rawMessage: String,
        referenceMillis: Long,
        fileName: String?,
    ): Load? = LoadCandidateRanker.chooseOne(loadGroups(rawMessage, referenceMillis, fileName))

    fun parseLoadFromMessage(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Result<Load> = runCatching {
        parseAmazonRelayLoad(rawMessage, referenceMillis)?.load
            ?: throw IllegalArgumentException("No valid load found in message")
    }

    /** Add-load screen: the parser whose numbers agree, not the first one that matched. */
    fun parseLoadFromUserInput(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Result<Load> = runCatching {
        bestLoad(rawMessage, referenceMillis, fileName)
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
        bestLoad(rawMessage, referenceMillis, fileName)?.let { return LoadDraftFields.fromLoad(it) }
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
