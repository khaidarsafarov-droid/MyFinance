package com.truckerload.domain.ingest

import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.MessageParseService

/**
 * Decides whether inbound Telegram file / paste text is a load that can be
 * saved immediately, or a receipt that still needs a type confirmation.
 */
object InboundDocumentResolver {

    data class Decision(
        val preview: ReceiptPreview,
        val loads: List<Load>,
        val autoSaveLoads: Boolean,
    )

    fun resolve(
        text: String,
        fileName: String? = null,
        messageDateSeconds: Long? = null,
        referenceMillis: Long = System.currentTimeMillis(),
        parseService: MessageParseService = MessageParseService(),
    ): Decision {
        val preview = ReceiptFieldExtractor.extract(text, fileName, messageDateSeconds)
        val loads = parseService.parseLoadsFromInboundText(text, referenceMillis, fileName)
            .getOrNull()
            .orEmpty()
        val scores = ReceiptKindClassifier.scores(combinedText(text, fileName))
        val loadScore = scores[ReceiptKind.LOAD] ?: 0
        val paycheckScore = scores[ReceiptKind.PAYCHECK] ?: 0
        val fuelScore = maxOf(scores[ReceiptKind.DIESEL] ?: 0, scores[ReceiptKind.DEF] ?: 0)
        val autoSaveLoads = loads.isNotEmpty() &&
            paycheckScore < loadScore &&
            (preview.kind == ReceiptKind.LOAD ||
                preview.kind == ReceiptKind.UNKNOWN ||
                loadScore >= 6 && loadScore >= fuelScore)
        return Decision(preview = preview, loads = loads, autoSaveLoads = autoSaveLoads)
    }

    internal fun combinedText(text: String, fileName: String?): String =
        listOfNotNull(fileName?.replace('-', ' '), text).joinToString("\n")
}
