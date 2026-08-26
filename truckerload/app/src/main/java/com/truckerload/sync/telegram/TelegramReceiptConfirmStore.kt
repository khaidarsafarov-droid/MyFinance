package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptPreview
import com.truckerload.utils.PaycheckSourceFiles
import org.json.JSONObject

class TelegramReceiptConfirmStore(
    private val prefs: SharedPreferences,
    private val context: Context? = null,
) {
    fun save(chatId: String, preview: ReceiptPreview) {
        prefs.edit {
            putString(key(chatId), toJson(preview).toString())
            putLong(timeKey(chatId), System.currentTimeMillis())
        }
    }

    fun load(chatId: String): ReceiptPreview? {
        val savedAt = prefs.getLong(timeKey(chatId), 0L)
        if (savedAt != 0L && System.currentTimeMillis() - savedAt > TTL_MS) {
            clear(chatId, discardFile = true)
            return null
        }
        val raw = prefs.getString(key(chatId), null) ?: return null
        return runCatching { fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun clear(chatId: String, discardFile: Boolean = false) {
        if (discardFile) discardStoredFile(chatId)
        prefs.edit {
            remove(key(chatId))
            remove(timeKey(chatId))
        }
    }

    private fun discardStoredFile(chatId: String) {
        val ctx = context ?: return
        val raw = prefs.getString(key(chatId), null) ?: return
        val path = runCatching { fromJson(JSONObject(raw)).sourceFilePath }.getOrNull()
        PaycheckSourceFiles.delete(ctx, path)
    }

    private fun key(chatId: String) = "tg_receipt_$chatId"
    private fun timeKey(chatId: String) = "tg_receipt_at_$chatId"

    private fun toJson(preview: ReceiptPreview) = JSONObject().apply {
        put("kind", preview.kind.name)
        put("amount", preview.amount ?: JSONObject.NULL)
        put("gallons", preview.gallons ?: JSONObject.NULL)
        put("ppg", preview.pricePerGallon ?: JSONObject.NULL)
        put("date", preview.date ?: JSONObject.NULL)
        put("location", preview.location ?: JSONObject.NULL)
        put("vendor", preview.vendor ?: JSONObject.NULL)
        put("driver", preview.driverName ?: JSONObject.NULL)
        put("trip", preview.tripId ?: JSONObject.NULL)
        put("miles", preview.miles ?: JSONObject.NULL)
        put("pointA", preview.pointA ?: JSONObject.NULL)
        put("pointB", preview.pointB ?: JSONObject.NULL)
        put("text", preview.extractedText.take(12_000))
        put("highlight", preview.highlightToken ?: JSONObject.NULL)
        put("file", preview.sourceFileName ?: JSONObject.NULL)
        put("filePath", preview.sourceFilePath ?: JSONObject.NULL)
        put("msgDate", preview.messageDateSeconds ?: JSONObject.NULL)
    }

    private fun fromJson(json: JSONObject) = ReceiptPreview(
        kind = runCatching { ReceiptKind.valueOf(json.optString("kind")) }
            .getOrDefault(ReceiptKind.UNKNOWN),
        amount = json.optDoubleOrNull("amount"),
        gallons = json.optDoubleOrNull("gallons"),
        pricePerGallon = json.optDoubleOrNull("ppg"),
        date = json.optStringOrNull("date"),
        location = json.optStringOrNull("location"),
        vendor = json.optStringOrNull("vendor"),
        driverName = json.optStringOrNull("driver"),
        tripId = json.optStringOrNull("trip"),
        miles = json.optDoubleOrNull("miles"),
        pointA = json.optStringOrNull("pointA"),
        pointB = json.optStringOrNull("pointB"),
        extractedText = json.optString("text"),
        highlightToken = json.optStringOrNull("highlight"),
        sourceFileName = json.optStringOrNull("file"),
        sourceFilePath = json.optStringOrNull("filePath"),
        messageDateSeconds = if (json.has("msgDate") && !json.isNull("msgDate")) {
            json.optLong("msgDate")
        } else {
            null
        },
    )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return value.takeIf { !it.isNaN() }
    }

    companion object {
        private const val TTL_MS = 30L * 60L * 1000L
    }
}
