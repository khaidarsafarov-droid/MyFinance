package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Structured JSON from broker APIs, TMS exports, or webhook dumps.
 * Vendor key names differ, so lookups are alias-based and case/underscore insensitive.
 */
object JsonLoadParser {

    private val tripKeys = listOf(
        "tripid", "trip", "loadid", "loadnumber", "ponumber", "po", "pronumber",
        "reference", "referencenumber", "ref", "confirmation", "confirmationnumber",
        "orderid", "ordernumber", "shipmentid", "bol", "number",
    )
    private val rateKeys = listOf(
        "totalrate", "linehaul", "linehaulrate", "allin", "allinrate", "carrierrate",
        "estimatedrate", "agreedrate", "rate", "revenue", "gross", "grossamount",
        "amount", "pay", "price", "total",
    )
    private val milesKeys = listOf(
        "totalloadedmiles", "loadedmiles", "totalmiles", "miles", "mileage", "distance",
    )
    private val pickupKeys = listOf(
        "puaddress", "pickupaddress", "pickuplocation", "pickup", "originaddress",
        "origin", "shipper", "from", "pu",
    )
    private val deliveryKeys = listOf(
        "deladdress", "deliveryaddress", "deliverylocation", "delivery",
        "destinationaddress", "destination", "consignee", "dropoff", "drop", "to", "del",
    )
    private val dateKeys = listOf(
        "pickupdate", "pudate", "shipdate", "startdate", "loaddate", "date",
    )
    private val addressPartKeys = listOf(
        "name", "facility", "facilitycode", "code", "address", "address1",
        "addressline1", "street", "city", "state", "zip", "postalcode",
    )
    private val collectionKeys = listOf(
        "loads", "shipments", "orders", "trips", "data", "results", "items", "records",
    )
    private val nestedKeys = listOf("load", "shipment", "order", "trip", "payload", "result")

    fun looksLikeJson(raw: String): Boolean {
        val text = raw.trim()
        if (text.length < 2) return false
        return (text.startsWith("{") && text.endsWith("}")) ||
            (text.startsWith("[") && text.endsWith("]"))
    }

    fun parseAll(raw: String, referenceMillis: Long = System.currentTimeMillis()): List<Load> {
        val root = rootOf(raw) ?: return emptyList()
        return when (root) {
            is JSONArray -> root.objects().flatMap { fromNode(it, raw, referenceMillis, 0) }
            is JSONObject -> fromNode(root, raw, referenceMillis, 0)
            else -> emptyList()
        }
    }

    fun parseOne(raw: String, referenceMillis: Long = System.currentTimeMillis()): Load? =
        parseAll(raw, referenceMillis).firstOrNull()

    /**
     * `key: value` dump of any JSON so the regex parsers can read payloads whose
     * key names are not in the alias tables.
     */
    fun flattenToText(raw: String): String {
        val root = rootOf(raw) ?: return ""
        val out = StringBuilder()
        flatten(root, out, 0)
        return out.toString().trim()
    }

    private fun rootOf(raw: String): Any? {
        val text = raw.trim()
        if (!looksLikeJson(text)) return null
        return runCatching {
            if (text.startsWith("[")) JSONArray(text) else JSONObject(text)
        }.getOrNull()
    }

    private fun fromNode(
        node: JSONObject,
        raw: String,
        referenceMillis: Long,
        depth: Int,
    ): List<Load> {
        if (depth > MAX_DEPTH) return emptyList()

        // Telegram Desktop export: the loads live inside chat message text.
        node.optJSONArray("messages")?.let { messages ->
            val text = messages.objects().joinToString("\n\n") { messageText(it) }
            return LoadMessageParser.parseAll(text, referenceMillis)
        }

        collectionKeys.firstNotNullOfOrNull { key -> node.optJSONArray(key) }?.let { array ->
            val nested = array.objects().flatMap { fromNode(it, raw, referenceMillis, depth + 1) }
            if (nested.isNotEmpty()) return nested
        }
        nestedKeys.firstNotNullOfOrNull { key -> node.optJSONObject(key) }?.let { child ->
            val nested = fromNode(child, raw, referenceMillis, depth + 1)
            if (nested.isNotEmpty()) return nested
        }
        return listOfNotNull(singleLoad(node, raw, referenceMillis))
    }

    private fun singleLoad(node: JSONObject, raw: String, referenceMillis: Long): Load? {
        val index = flatIndex(node)
        val rate = numberFor(index, rateKeys) ?: return null
        if (rate <= 0.0) return null
        val (pickup, delivery) = routeOf(node, index)
        if (pickup.isBlank() && delivery.isBlank()) return null

        val miles = numberFor(index, milesKeys)?.let { ParseUtils.sanitizeLoadedMiles(it, rate) } ?: 0.0
        val tripId = textFor(index, tripKeys)?.uppercase(Locale.US).orEmpty()
        val date = textFor(index, dateKeys)
            ?.let { ParseUtils.normalizeDate(it, referenceMillis = referenceMillis) }
            .orEmpty()

        return ManualLoadFactory.build(
            tripId = tripId,
            date = date,
            rate = rate,
            miles = miles,
            pointA = pickup,
            pointB = delivery,
            rawMessage = raw.take(MAX_RAW_CHARS),
            nowMillis = referenceMillis,
        )
    }

    private fun routeOf(node: JSONObject, index: Map<String, Any>): Pair<String, String> {
        val labeledPickup = index.entryFor(pickupKeys)?.let { addressOf(it) }.orEmpty()
        val labeledDelivery = index.entryFor(deliveryKeys)?.let { addressOf(it) }.orEmpty()
        if (labeledPickup.isNotBlank() || labeledDelivery.isNotBlank()) {
            return labeledPickup to labeledDelivery
        }
        val stops = node.optJSONArray("stops")?.objects()
            ?.map { addressOf(it) }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        return when {
            stops.size >= 2 -> stops.first() to stops.last()
            stops.size == 1 -> stops.first() to ""
            else -> "" to ""
        }
    }

    private fun addressOf(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value.trim()
        is JSONArray -> value.values().joinToString(", ") { addressOf(it) }.trim(' ', ',')
        is JSONObject -> {
            val parts = addressPartKeys.mapNotNull { key ->
                value.keys().asSequence()
                    .firstOrNull { normalize(it) == key }
                    ?.let { value.optString(it).trim().takeIf(String::isNotBlank) }
            }
            parts.distinct().joinToString(", ")
        }
        else -> value.toString().trim()
    }

    /** Flat `normalizedKey -> value` view so nested vendor payloads resolve by alias. */
    private fun flatIndex(node: JSONObject): Map<String, Any> {
        val index = LinkedHashMap<String, Any>()
        fun walk(current: JSONObject, depth: Int) {
            if (depth > MAX_DEPTH) return
            for (key in current.keys()) {
                val value = current.opt(key) ?: continue
                if (value === JSONObject.NULL) continue
                index.putIfAbsent(normalize(key), value)
                if (value is JSONObject) walk(value, depth + 1)
            }
        }
        walk(node, 0)
        return index
    }

    private fun Map<String, Any>.entryFor(aliases: List<String>): Any? =
        aliases.firstNotNullOfOrNull { alias -> this[alias] }

    private fun numberFor(index: Map<String, Any>, aliases: List<String>): Double? {
        val value = index.entryFor(aliases) ?: return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> ParseUtils.parseMoney(value).takeIf { it > 0.0 }
            is JSONObject -> value.keys().asSequence()
                .firstOrNull { normalize(it) in setOf("amount", "value", "total") }
                ?.let { ParseUtils.parseMoney(value.optString(it)) }
                ?.takeIf { it > 0.0 }
            else -> null
        }
    }

    private fun textFor(index: Map<String, Any>, aliases: List<String>): String? {
        val value = index.entryFor(aliases) ?: return null
        return when (value) {
            is String -> value.trim().takeIf { it.isNotBlank() }
            is Number -> value.toString()
            else -> null
        }
    }

    private fun messageText(message: JSONObject): String = when (val value = message.opt("text")) {
        is String -> value
        is JSONArray -> value.values().joinToString("") { item ->
            when (item) {
                is String -> item
                is JSONObject -> item.optString("text")
                else -> ""
            }
        }
        else -> ""
    }

    private fun flatten(value: Any?, out: StringBuilder, depth: Int) {
        if (depth > MAX_DEPTH) return
        when (value) {
            is JSONObject -> for (key in value.keys()) {
                val child = value.opt(key)
                if (child is JSONObject || child is JSONArray) {
                    flatten(child, out, depth + 1)
                } else if (child != null && child !== JSONObject.NULL) {
                    out.append(key).append(": ").append(child).append('\n')
                }
            }
            is JSONArray -> value.values().forEach { flatten(it, out, depth + 1) }
            else -> Unit
        }
    }

    private fun normalize(key: String): String =
        key.lowercase(Locale.US).filter { it.isLetterOrDigit() }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray.values(): List<Any> =
        (0 until length()).mapNotNull { opt(it) }.filter { it !== JSONObject.NULL }

    private const val MAX_DEPTH = 6
    private const val MAX_RAW_CHARS = 4000
}
