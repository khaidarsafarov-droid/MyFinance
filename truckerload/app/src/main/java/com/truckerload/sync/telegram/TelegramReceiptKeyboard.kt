package com.truckerload.sync.telegram

import org.json.JSONArray
import org.json.JSONObject

object TelegramReceiptKeyboard {
    const val LOAD = "rc:load"
    const val DIESEL = "rc:diesel"
    const val DEF = "rc:def"
    const val PAYCHECK = "rc:pay"
    const val CONFIRM = "rc:ok"
    const val CANCEL = "rc:cancel"
    const val TYPE_AMOUNT = "rc:type"
    const val AMOUNT_PREFIX = "rc:amt:"

    fun isReceiptCallback(data: String): Boolean = data.startsWith("rc:")

    fun amountCallback(amount: Double): String =
        AMOUNT_PREFIX + "%.2f".format(java.util.Locale.US, amount)

    fun parseAmountCallback(data: String): Double? {
        if (!data.startsWith(AMOUNT_PREFIX)) return null
        return data.removePrefix(AMOUNT_PREFIX).toDoubleOrNull()?.takeIf { it > 0 }
    }

    fun confirm(yes: String, no: String): JSONObject =
        JSONObject().apply {
            put(
                "inline_keyboard",
                JSONArray().apply {
                    put(
                        JSONArray().apply {
                            put(button(yes, CONFIRM))
                            put(button(no, CANCEL))
                        },
                    )
                },
            )
        }

    fun amountPicker(
        amounts: List<Double>,
        yes: String,
        typeOwn: String,
        cancel: String?,
    ): JSONObject =
        JSONObject().apply {
            put(
                "inline_keyboard",
                JSONArray().apply {
                    val unique = amounts.filter { it > 0 }.distinctBy { "%.2f".format(java.util.Locale.US, it) }
                    if (unique.size > 1) {
                        unique.chunked(2).forEach { row ->
                            put(
                                JSONArray().apply {
                                    row.forEach { amount ->
                                        val label = "$" + "%,.2f".format(java.util.Locale.US, amount)
                                        put(button(label, amountCallback(amount)))
                                    }
                                },
                            )
                        }
                    }
                    put(
                        JSONArray().apply {
                            if (yes.isNotBlank()) put(button(yes, CONFIRM))
                            put(button(typeOwn, TYPE_AMOUNT))
                        },
                    )
                    if (!cancel.isNullOrBlank()) {
                        put(JSONArray().apply { put(button(cancel, CANCEL)) })
                    }
                },
            )
        }

    fun inline(
        load: String,
        diesel: String,
        def: String,
        paycheck: String,
        cancel: String,
    ): JSONObject =
        JSONObject().apply {
            put(
                "inline_keyboard",
                JSONArray().apply {
                    put(
                        JSONArray().apply {
                            put(button(load, LOAD))
                            put(button(diesel, DIESEL))
                        },
                    )
                    put(
                        JSONArray().apply {
                            put(button(def, DEF))
                            put(button(paycheck, PAYCHECK))
                        },
                    )
                    put(
                        JSONArray().apply {
                            put(button(cancel, CANCEL))
                        },
                    )
                },
            )
        }

    private fun button(label: String, data: String) = JSONObject()
        .put("text", label)
        .put("callback_data", data)
}
