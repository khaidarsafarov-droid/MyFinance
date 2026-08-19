package com.truckerload.domain.ingest

import com.truckerload.domain.parser.DieselTextParser
import com.truckerload.domain.parser.LoadMessageParser
import com.truckerload.domain.parser.PaycheckTextParser
import com.truckerload.domain.parser.ServiceReceiptTextParser
import java.util.Locale

/**
 * Pulls journal fields from OCR / file text using the same parsers as in-app add flows.
 */
object ReceiptFieldExtractor {

    private val moneyToken = Regex("""\$\s*([\d,]+(?:\.\d{2})?)""")
    private val gallonsToken = Regex("""([\d]+(?:\.\d+)?)\s*(?:gal|gallons|гл)\b""", RegexOption.IGNORE_CASE)
    private val tripToken = Regex("""\b(T-[A-Z0-9]{6,})\b""", RegexOption.IGNORE_CASE)

    fun extract(
        text: String,
        fileName: String? = null,
        messageDateSeconds: Long? = null,
    ): ReceiptPreview {
        val kind = ReceiptKindClassifier.classify(text)
        val paycheck = PaycheckTextParser.parse(text)
        val diesel = DieselTextParser.parse(text)
        val receipt = ServiceReceiptTextParser.parse(text)
        val loads = runCatching { LoadMessageParser.parseAll(text) }.getOrNull().orEmpty()
        val load = loads.firstOrNull()

        val amount = when (kind) {
            ReceiptKind.PAYCHECK -> paycheck?.netAmount ?: receipt.amount ?: firstMoney(text)
            ReceiptKind.DIESEL, ReceiptKind.DEF ->
                diesel?.totalAmount ?: receipt.amount ?: firstMoney(text)
            ReceiptKind.LOAD -> load?.totalRate?.takeIf { it > 0 } ?: firstMoney(text)
            ReceiptKind.UNKNOWN ->
                paycheck?.netAmount ?: diesel?.totalAmount ?: receipt.amount ?: firstMoney(text)
        }

        val gallons = diesel?.gallons ?: gallonsToken.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val ppg = diesel?.pricePerGallon
        val date = paycheck?.weekStartDate ?: diesel?.date ?: receipt.date?.takeIf { it.isNotBlank() }
        val location = diesel?.location ?: receipt.serviceName
        val vendor = diesel?.vendor ?: receipt.serviceName
        val highlight = amount?.let { formatMoneyToken(it, text) }

        return ReceiptPreview(
            kind = if (kind == ReceiptKind.UNKNOWN && load != null) ReceiptKind.LOAD else kind,
            amount = amount?.takeIf { it > 0 },
            gallons = gallons,
            pricePerGallon = ppg,
            date = date?.takeIf { it.isNotBlank() },
            location = location?.takeIf { it.isNotBlank() },
            vendor = vendor?.takeIf { it.isNotBlank() },
            driverName = paycheck?.driverName,
            tripId = load?.tripId?.takeIf { it.isNotBlank() }
                ?: tripToken.find(text)?.groupValues?.get(1),
            extractedText = text.trim(),
            highlightToken = highlight,
            sourceFileName = fileName,
            messageDateSeconds = messageDateSeconds,
        )
    }

    private fun firstMoney(text: String): Double? {
        val values = moneyToken.findAll(text).mapNotNull { match ->
            parseMoney(match.groupValues[1]).takeIf { it > 0 }
        }.toList()
        return values.lastOrNull()
    }

    private fun parseMoney(raw: String): Double {
        val cleaned = raw.trim().replace("$", "").replace(",", "").replace(" ", "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun formatMoneyToken(amount: Double, text: String): String? {
        val cents = String.format(Locale.US, "%.2f", amount)
        val whole = String.format(Locale.US, "%.0f", amount)
        val candidates = listOf(
            "$$cents",
            cents,
            "$$whole",
            whole,
            cents.replace(".", ","),
        )
        return candidates.firstOrNull { token ->
            text.contains(token, ignoreCase = true)
        }
    }
}
