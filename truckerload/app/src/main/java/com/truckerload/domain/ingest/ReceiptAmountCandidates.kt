package com.truckerload.domain.ingest

import com.truckerload.domain.parser.DieselReceiptExtractor
import com.truckerload.domain.parser.DieselTextParser
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.PaycheckTextParser
import com.truckerload.domain.paycheck.PaycheckSalaryFields
import kotlin.math.abs

/** Labeled dollar amounts the bot can offer when a statement has many totals. */
data class ReceiptAmountChoice(
    val amount: Double,
    val label: String,
)

object ReceiptAmountCandidates {

    fun paycheck(text: String, fileName: String? = null, preferred: Double? = null): List<ReceiptAmountChoice> {
        val parsed = PaycheckTextParser.parse(text, fileName)
        val hay = listOfNotNull(fileName?.replace('-', ' '), text).joinToString("\n")
        val found = mutableListOf<ReceiptAmountChoice>()
        fun add(label: String, amount: Double?) {
            if (amount == null || amount <= 0) return
            if (found.any { near(it.amount, amount) }) return
            found += ReceiptAmountChoice(amount, label)
        }
        add("take-home", parsed?.netAmount)
        add("gross", parsed?.grossAmount)
        for ((label, pattern) in PAYCHECK_LABELS) {
            add(label, firstLabeled(hay, pattern))
        }
        return order(found, preferred).take(MAX_CHOICES)
    }

    fun diesel(text: String, preferred: Double? = null): List<ReceiptAmountChoice> {
        val parsed = DieselTextParser.parse(text)
        val fields = DieselReceiptExtractor.extract(text)
        val found = mutableListOf<ReceiptAmountChoice>()
        fun add(label: String, amount: Double?) {
            if (amount == null || amount <= 0) return
            if (found.any { near(it.amount, amount) }) return
            found += ReceiptAmountChoice(amount, label)
        }
        add("total", preferred ?: parsed?.totalAmount ?: fields.totalAmount)
        add("pump", firstLabeled(text, PUMP_TOTAL))
        add("fuel", firstLabeled(text, FUEL_AMOUNT))
        add("sale", firstLabeled(text, SALE_TOTAL))
        val gallons = parsed?.gallons ?: fields.gallons
        val ppg = parsed?.pricePerGallon ?: fields.pricePerGallon
        if (gallons != null && ppg != null) {
            add("gallons × ppg", gallons * ppg)
        }
        return order(found, preferred ?: parsed?.totalAmount).take(MAX_CHOICES)
    }

    fun forPreview(preview: ReceiptPreview): List<ReceiptAmountChoice> {
        val text = preview.extractedText
        val file = preview.sourceFileName
        return when (preview.kind) {
            ReceiptKind.PAYCHECK -> paycheck(text, file, preview.amount)
            ReceiptKind.DIESEL, ReceiptKind.DEF -> diesel(text, preview.amount)
            else -> preview.amount?.let { listOf(ReceiptAmountChoice(it, "total")) }.orEmpty()
        }
    }

    /**
     * A short reply that is only an amount the user typed, not a new Relay load
     * or a full statement paste.
     */
    fun parseTypedAmount(text: String): Double? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        if (MessageClassifier.isLoadLike(trimmed)) return null
        val lines = trimmed.lines().count { it.isNotBlank() }
        if (lines > 4 || trimmed.length > 80) return null
        PaycheckSalaryFields.parseAmount(trimmed)?.let { return it }
        val token = TYPED_MONEY.find(trimmed)?.groupValues?.getOrNull(1) ?: return null
        return PaycheckSalaryFields.parseAmount(token)
    }

    private fun firstLabeled(text: String, label: String): Double? {
        val rx = Regex(
            """$label[^\d${'$'}]{0,60}${'$'}?\s*([\d]{1,3}(?:,\d{3})+(?:\.\d{2})?|[\d]+\.\d{2})""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val raw = rx.find(text)?.groupValues?.getOrNull(1) ?: return null
        return PaycheckSalaryFields.parseAmount(raw)
    }

    private fun order(
        found: List<ReceiptAmountChoice>,
        preferred: Double?,
    ): List<ReceiptAmountChoice> {
        if (preferred == null || preferred <= 0) return found
        val head = found.firstOrNull { near(it.amount, preferred) }
            ?: ReceiptAmountChoice(preferred, "chosen")
        return listOf(head) + found.filter { !near(it.amount, preferred) }
    }

    private fun near(a: Double, b: Double): Boolean = abs(a - b) < 0.015

    private const val MAX_CHOICES = 4
    private val TYPED_MONEY = Regex(
        """[${'$'}]?\s*([\d]{1,3}(?:,\d{3})+\.\d{2}|[\d]{1,3}(?:,\d{3})+|[\d]+\.\d{2}|[\d]{2,6})""",
    )
    private val PUMP_TOTAL = """Pump\s*Total"""
    private val FUEL_AMOUNT = """Fuel\s*(?:Amount|Sale|Total)|Diesel\s*Total|Total\s*Amount"""
    private val SALE_TOTAL = """(?:^|\n)\s*(?:SALE|Sale\s*(?:Amount|Total))"""
    private val PAYCHECK_LABELS = listOf(
        "Grand Total" to """G[ra]{0,2}and\s*Tota[l1I]""",
        "Net Settlement" to """Net\s*(?:Settlement|Earnings|Compensation)""",
        "Take Home" to """Take[\s-]*Home|Check\s*(?:Amount|Total)""",
        "Settlement Total" to """Settlement\s*(?:Total|Amount)""",
    )
}
