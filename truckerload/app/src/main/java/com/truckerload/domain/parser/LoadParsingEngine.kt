package com.truckerload.domain.parser

import com.truckerload.domain.model.Load

/**
 * The only public load-parsing entry. Every channel runs the same ranker:
 * each parser proposes candidates, and [LoadCandidateRanker] keeps the group
 * whose rate, miles, and stops agree. Source metadata on [ParseInput] does not
 * pick a parser.
 */
object LoadParsingEngine {

    fun parse(input: ParseInput): Result<List<Load>> = runCatching {
        val chosen = LoadCandidateRanker.choose(
            candidateGroups(input.text, input.referenceMillis, input.fileName),
        )
        if (LoadCandidateRanker.keepDespiteReceipt(MessageClassifier.classify(input.text), chosen)) {
            chosen
        } else {
            emptyList()
        }
    }

    /** Ranked draft for form fill. Skips the paycheck/diesel gate so a partial paste still shows fields. */
    internal fun rankedLoad(input: ParseInput): Load? =
        LoadCandidateRanker.chooseOne(candidateGroups(input.text, input.referenceMillis, input.fileName))

    /** Every parser that can read this text. The ranker, not call order, decides the winner. */
    private fun candidateGroups(
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
}
