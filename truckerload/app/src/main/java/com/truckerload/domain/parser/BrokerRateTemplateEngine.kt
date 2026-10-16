package com.truckerload.domain.parser

import com.truckerload.domain.money.Money
import com.truckerload.domain.model.Load
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Broker rate-confirmation templates tried beside [RateConfirmationLoadParser].
 * A template wins only when its keywords match and it finds a rate. Stops still
 * come from the existing parser, and a miss falls through to that parser.
 *
 * Field labels for the named brokers are adapted from Sensible templates (MIT).
 * This is not the Sensible service.
 */
internal object BrokerRateTemplateEngine {

    fun parseOne(
        rawMessage: String,
        nowMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Load? {
        val text = TelegramStyledTextNormalizer.normalize(rawMessage.replace("\r\n", "\n").trim())
        if (text.isBlank()) return null
        val template = BrokerRateTemplateCatalog.bestMatch(text, fileName) ?: return null
        val rate = firstAmount(template.rate, text) ?: return null
        if (rate <= 0.0) return null
        val (pointA, pointB) = RateConfirmationLoadParser.extractStops(text)
        val tripId = firstToken(template.tripId, text).orEmpty()
        if (pointA.isBlank() && pointB.isBlank()) return null
        val miles = milesPattern.find(text)?.groupValues?.get(1)
            ?.let { ParseUtils.parseMiles(it) }
            ?.let { ParseUtils.sanitizeLoadedMiles(it, Money.fromDollars(rate)) }
            ?: 0.0
        val date = pickupDatePattern.find(text)?.groupValues?.get(1)
            ?.let { ParseUtils.normalizeDate(it, referenceMillis = nowMillis) }
            .orEmpty()
        return ManualLoadFactory.build(
            tripId = tripId,
            date = date,
            rate = rate,
            miles = miles,
            pointA = pointA,
            pointB = pointB,
            rawMessage = rawMessage,
            nowMillis = nowMillis,
        )
    }

    /**
     * Linehaul + fuel surcharge + accessorials, in cents, compared with the labeled total.
     * Null when any piece is missing. A mismatch does not drop the load.
     */
    fun partsAgreeWithTotal(text: String): Boolean? {
        val check = ratePartCheck(text) ?: return null
        return check.labeledCents == check.partsCents
    }

    /** Labeled total and the sum of linehaul, fuel, and accessorials. Null if any piece is missing. */
    fun ratePartCheck(text: String): RatePartCheck? {
        val template = BrokerRateTemplateCatalog.bestMatch(text, null) ?: return null
        val total = firstAmount(template.rate, text) ?: return null
        val linehaul = firstAmount(template.linehaul, text) ?: return null
        val fuel = firstAmount(template.fuel, text) ?: return null
        val accessorials = firstAmount(template.accessorials, text) ?: return null
        val sum = Money.fromDollars(linehaul).cents +
            Money.fromDollars(fuel).cents +
            Money.fromDollars(accessorials).cents
        return RatePartCheck(
            labeledCents = Money.fromDollars(total).cents,
            partsCents = sum,
        )
    }

    private fun firstAmount(rules: List<FieldRule>, text: String): Double? {
        for (rule in rules) {
            val value = amount(rule, text)?.takeIf { it > 0.0 } ?: continue
            return value
        }
        return null
    }

    private fun firstToken(rules: List<FieldRule>, text: String): String? {
        for (rule in rules) {
            val token = token(rule, text)?.takeIf { it.isNotBlank() } ?: continue
            return token.uppercase(Locale.US)
        }
        return null
    }

    private fun amount(rule: FieldRule, text: String): Double? {
        val scope = scope(text, rule.section)
        return when (rule.type) {
            RULE_LINE -> lastMoneyOnLabeledLine(scope, rule.label)
            RULE_AFTER -> moneyAfter(scope, rule.label)
            else -> null
        }
    }

    private fun token(rule: FieldRule, text: String): String? {
        if (rule.type != RULE_AFTER || rule.label.isBlank()) return null
        val scope = scope(text, rule.section)
        val at = indexOfLabel(scope, rule.label) ?: return null
        val rest = scope.substring(at + rule.label.length).take(80)
        return tokenPattern.find(rest)?.groupValues?.get(1)
    }

    private fun lastMoneyOnLabeledLine(text: String, label: String): Double? {
        if (label.isBlank()) return null
        var found: Double? = null
        for (line in text.lines()) {
            if (indexOfLabel(line, label) == null) continue
            val amounts = money.findAll(line)
                .map { ParseUtils.parseMoney(it.groupValues[1]) }
                .filter { it > 0.0 }
                .toList()
            if (amounts.isNotEmpty()) found = amounts.last()
        }
        return found
    }

    private fun moneyAfter(text: String, label: String): Double? {
        val at = indexOfLabel(text, label) ?: return null
        val rest = text.substring(at + label.length).lineSequence().firstOrNull().orEmpty()
        val raw = money.find(rest)?.groupValues?.get(1) ?: return null
        return ParseUtils.parseMoney(raw).takeIf { it > 0.0 }
    }

    private fun scope(text: String, section: String?): String {
        if (section.isNullOrBlank()) return text
        val at = indexOfLabel(text, section) ?: return ""
        return text.substring(at).take(SECTION_CHARS)
    }

    private fun indexOfLabel(text: String, label: String): Int? {
        val at = text.indexOf(label, ignoreCase = true)
        return at.takeIf { it >= 0 }
    }

    private val money = Regex("""\$?\s*([\d,]+(?:\.\d{2})?)""")
    private val tokenPattern = Regex("""([A-Za-z0-9][A-Za-z0-9\-]{3,})""")
    private val milesPattern = Regex("""(?:Total\s*Loaded\s*)?Miles\s*:\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val pickupDatePattern = Regex(
        """Pick[\s\-]*Up\s*:\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""",
        RegexOption.IGNORE_CASE,
    )

    private const val RULE_LINE = "lineAmount"
    private const val RULE_AFTER = "after"
    private const val SECTION_CHARS = 1_600
}

internal data class RatePartCheck(
    val labeledCents: Long,
    val partsCents: Long,
)

internal data class FieldRule(
    val type: String,
    val label: String,
    val section: String?,
)

internal data class BrokerTemplate(
    val id: String,
    val matchAny: List<String>,
    val matchAll: List<String>,
    val tripId: List<FieldRule>,
    val rate: List<FieldRule>,
    val linehaul: List<FieldRule>,
    val fuel: List<FieldRule>,
    val accessorials: List<FieldRule>,
)

internal object BrokerRateTemplateCatalog {

    fun bestMatch(text: String, fileName: String?): BrokerTemplate? {
        val haystack = listOfNotNull(fileName, text).joinToString("\n")
        return templates
            .filter { matches(it, haystack) }
            .maxByOrNull { specificity(it) }
    }

    fun size(): Int = templates.size

    private fun matches(template: BrokerTemplate, text: String): Boolean {
        if (template.matchAll.any { !text.contains(it, ignoreCase = true) }) return false
        if (template.matchAny.isEmpty()) return template.matchAll.isNotEmpty()
        return template.matchAny.any { text.contains(it, ignoreCase = true) }
    }

    private fun specificity(template: BrokerTemplate): Int =
        template.matchAll.sumOf { it.length } + (template.matchAny.maxOfOrNull { it.length } ?: 0)

    private val templates: List<BrokerTemplate> by lazy { load() }

    private fun load(): List<BrokerTemplate> {
        val raw = BrokerRateTemplateCatalog::class.java.classLoader
            ?.getResourceAsStream(RESOURCE)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching { parse(raw) }.getOrDefault(emptyList())
    }

    private fun parse(raw: String): List<BrokerTemplate> {
        val array = JSONObject(raw).optJSONArray("templates") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val rate = rules(item, "rate")
                if (rate.isEmpty()) continue
                add(
                    BrokerTemplate(
                        id = item.optString("id"),
                        matchAny = strings(item, "matchAny"),
                        matchAll = strings(item, "matchAll"),
                        tripId = rules(item, "tripId"),
                        rate = rate,
                        linehaul = rules(item, "linehaul"),
                        fuel = rules(item, "fuel"),
                        accessorials = rules(item, "accessorials"),
                    ),
                )
            }
        }
    }

    private fun strings(item: JSONObject, name: String): List<String> {
        val array = item.optJSONArray(name) ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                array.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    private fun rules(item: JSONObject, name: String): List<FieldRule> {
        val array = item.optJSONArray(name) ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val rule = array.optJSONObject(i) ?: continue
                val label = rule.optString("label")
                if (label.isBlank()) continue
                add(
                    FieldRule(
                        type = rule.optString("type"),
                        label = label,
                        section = rule.optString("section").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    private const val RESOURCE = "rate_broker_templates.json"
}
