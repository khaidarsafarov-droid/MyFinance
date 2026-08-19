package com.truckerload.sync.telegram

import org.json.JSONArray
import org.json.JSONObject

object TelegramReceiptKeyboard {
    const val DIESEL = "rc:diesel"
    const val DEF = "rc:def"
    const val PAYCHECK = "rc:pay"
    const val CANCEL = "rc:cancel"

    fun isReceiptCallback(data: String): Boolean = data.startsWith("rc:")

    fun inline(diesel: String, def: String, paycheck: String, cancel: String): JSONObject =
        JSONObject().apply {
            put(
                "inline_keyboard",
                JSONArray().apply {
                    put(
                        JSONArray().apply {
                            put(button(diesel, DIESEL))
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
