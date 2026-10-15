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

    fun amountLabel(amount: Double): String =
        "$" + "%,.2f".format(java.util.Locale.US, amount)

    /**
     * One-time reply keyboard. A tap sends the label as a normal message, which the
     * long poll already receives. Inline callback presses on this step were not
     * producing a chat reply.
     */
    fun amountPicker(
        amounts: List<Double>,
        yes: String,
        typeOwn: String,
        cancel: String?,
    ): JSONObject =
        JSONObject().apply {
            put("keyboard", amountKeyRows(amounts, yes, typeOwn, cancel))
            put("resize_keyboard", true)
            put("one_time_keyboard", true)
        }

    private fun amountKeyRows(
        amounts: List<Double>,
        yes: String,
        typeOwn: String,
        cancel: String?,
    ): JSONArray = JSONArray().apply {
        val unique = amounts.filter { it > 0 }.distinctBy { "%.2f".format(java.util.Locale.US, it) }
        if (unique.size > 1) {
            unique.chunked(2).forEach { row ->
                put(
                    JSONArray().apply {
                        row.forEach { amount ->
                            put(JSONObject().put("text", amountLabel(amount)))
                        }
                    },
                )
            }
        }
        put(
            JSONArray().apply {
                if (yes.isNotBlank()) put(JSONObject().put("text", yes))
                put(JSONObject().put("text", typeOwn))
            },
        )
        if (!cancel.isNullOrBlank()) {
            put(JSONArray().apply { put(JSONObject().put("text", cancel)) })
        }
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
                "keyboard",
                JSONArray().apply {
                    put(textRow(load, diesel))
                    put(textRow(def, paycheck))
                    put(textRow(cancel))
                },
            )
            put("resize_keyboard", true)
            put("one_time_keyboard", true)
        }

    private fun textRow(vararg labels: String): JSONArray = JSONArray().apply {
        labels.forEach { put(JSONObject().put("text", it)) }
    }

    private fun button(label: String, data: String) = JSONObject()
        .put("text", label)
        .put("callback_data", data)
}
