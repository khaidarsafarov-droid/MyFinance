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
        val ranked = LoadParsingEngine.rankedLoad(input)?.let { LoadDraftFields.fromLoad(it) }
        val direct = FlexibleLoadParser.extractFields(rawMessage)
        val existing = (ranked ?: direct).dropImplausible()
        // Partial Relay text (e.g. no rate yet): keep every field that was found, never reject.
        val relay = LoadMessageParser.parseDraft(rawMessage, referenceMillis)?.let { LoadDraftFields.fromLoad(it) }
        val chat = if (relay == null) chatDraft(rawMessage, referenceMillis) else null
        val generic = LoadDraftFields.fromExtracted(RateConTextExtractor.extract(rawMessage))
        val others = listOfNotNull(relay, chat, ranked, direct, generic).map { it.dropImplausible() }
        val merged = if (existing.isComplete() && existing.hasValidPlaces()) {
            // A complete, valid answer from the existing parsers is kept as is; the other schemes only fill blanks.
            val filled = others.fold(existing) { best, next -> best.fillBlanksFrom(next) }
            // A ranked draft without a date in the text carries "today"; a date the text really has is better.
            val defaulted = existing.date.isBlank() || existing.date == todayIso(referenceMillis)
            val date = chat?.date.orEmpty().ifBlank { if (defaulted) direct.date else "" }.ifBlank { filled.date }
            filled.copy(date = date)
        } else {
            // Otherwise the most complete valid value per field wins, specialised schemes first. A date found in
            // the text wins; the parsers that default to "today" only fill what is left.
            val best = others.reduceOrNull { first, next -> first.fillBlanksFrom(next) }
            val date = chat?.date.orEmpty().ifBlank { direct.date }.ifBlank { relay?.date.orEmpty() }
            best?.copy(date = date.ifBlank { best.date })
        }
        if (merged != null && !merged.isEmpty()) return merged
        val flattened = JsonLoadParser.flattenToText(rawMessage)
        return if (flattened.isBlank()) direct else FlexibleLoadParser.extractFields(flattened)
    }

    private fun todayOf(referenceMillis: Long): java.time.LocalDate =
        java.time.Instant.ofEpochMilli(referenceMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

    private fun todayIso(referenceMillis: Long): String = todayOf(referenceMillis).toString()

    private fun chatDraft(rawMessage: String, referenceMillis: Long): LoadDraftFields? =
        ChatLoadParser.extract(rawMessage, todayOf(referenceMillis))?.let { LoadDraftFields.fromExtracted(it) }

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
