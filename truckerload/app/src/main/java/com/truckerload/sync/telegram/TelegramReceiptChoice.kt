package com.truckerload.sync.telegram

import com.truckerload.domain.ingest.ReceiptAmountCandidates

/**
 * A tap on the amount keyboard arrives as the button label (a normal message).
 * Inline callback data (`rc:…`) is still accepted for a keyboard already on screen.
 */
internal object TelegramReceiptChoice {
    enum class Kind { Confirm, Cancel, TypeOwn, Amount, Load, Diesel, Def, Paycheck }

    data class Parsed(val kind: Kind, val amount: Double? = null) {
        fun toCallbackData(): String? = when (kind) {
            Kind.Confirm -> TelegramReceiptKeyboard.CONFIRM
            Kind.Cancel -> TelegramReceiptKeyboard.CANCEL
            Kind.TypeOwn -> TelegramReceiptKeyboard.TYPE_AMOUNT
            Kind.Amount -> amount?.takeIf { it > 0.0 }?.let(TelegramReceiptKeyboard::amountCallback)
            Kind.Load -> TelegramReceiptKeyboard.LOAD
            Kind.Diesel -> TelegramReceiptKeyboard.DIESEL
            Kind.Def -> TelegramReceiptKeyboard.DEF
            Kind.Paycheck -> TelegramReceiptKeyboard.PAYCHECK
        }
    }

    fun matchLabel(
        text: String,
        yes: String,
        typeOwn: String,
        cancel: String,
        load: String = "",
        diesel: String = "",
        defLabel: String = "",
        paycheck: String = "",
    ): Parsed? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("/")) return null
        if (yes.isNotBlank() && trimmed.equals(yes, ignoreCase = true)) return Parsed(Kind.Confirm)
        if (typeOwn.isNotBlank() && trimmed.equals(typeOwn, ignoreCase = true)) return Parsed(Kind.TypeOwn)
        if (cancel.isNotBlank() && trimmed.equals(cancel, ignoreCase = true)) return Parsed(Kind.Cancel)
        if (load.isNotBlank() && trimmed.equals(load, ignoreCase = true)) return Parsed(Kind.Load)
        if (diesel.isNotBlank() && trimmed.equals(diesel, ignoreCase = true)) return Parsed(Kind.Diesel)
        if (defLabel.isNotBlank() && trimmed.equals(defLabel, ignoreCase = true)) return Parsed(Kind.Def)
        if (paycheck.isNotBlank() && trimmed.equals(paycheck, ignoreCase = true)) return Parsed(Kind.Paycheck)
        if (trimmed.length > 32) return null
        val amount = ReceiptAmountCandidates.parseTypedAmount(trimmed) ?: return null
        return Parsed(Kind.Amount, amount)
    }

    fun receiptActionData(raw: String, yes: String, typeOwn: String, cancel: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (TelegramReceiptKeyboard.isReceiptCallback(text)) return text
        return matchLabel(text, yes, typeOwn, cancel)?.toCallbackData()
    }
}
