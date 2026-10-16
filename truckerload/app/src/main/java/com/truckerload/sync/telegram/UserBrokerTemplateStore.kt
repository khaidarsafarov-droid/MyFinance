package com.truckerload.sync.telegram

import android.content.Context
import androidx.core.content.edit
import com.truckerload.domain.parser.BrokerRateTemplateCatalog
import com.truckerload.domain.parser.BrokerTemplate
import com.truckerload.domain.parser.FieldRule
import org.json.JSONArray
import org.json.JSONObject

/** Driver-taught broker labels. Built-in templates still win when they match. */
internal object UserBrokerTemplateStore {

    fun publish(context: Context) {
        BrokerRateTemplateCatalog.userTemplates = load(context)
    }

    fun add(context: Context, template: BrokerTemplate) {
        val next = (listOf(template) + load(context).filter { it.matchAll != template.matchAll })
            .take(MAX)
        save(context, next)
        BrokerRateTemplateCatalog.userTemplates = next
    }

    private fun load(context: Context): List<BrokerTemplate> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val trip = item.optString("tripLabel")
                val rate = item.optString("rateLabel")
                if (trip.isBlank() || rate.isBlank()) continue
                add(template(trip, rate))
            }
        }
    }

    private fun save(context: Context, templates: List<BrokerTemplate>) {
        val array = JSONArray()
        templates.forEach { template ->
            val trip = template.tripId.firstOrNull()?.label ?: return@forEach
            val rate = template.rate.firstOrNull()?.label ?: return@forEach
            array.put(JSONObject().put("tripLabel", trip).put("rateLabel", rate))
        }
        prefs(context).edit { putString(KEY, array.toString()) }
    }

    fun template(tripLabel: String, rateLabel: String): BrokerTemplate = BrokerTemplate(
        id = "user-" + tripLabel.lowercase() + "-" + rateLabel.lowercase(),
        matchAny = emptyList(),
        matchAll = listOf(tripLabel, rateLabel),
        tripId = listOf(FieldRule(type = "after", label = tripLabel, section = null)),
        rate = listOf(FieldRule(type = "lineAmount", label = rateLabel, section = null)),
        linehaul = emptyList(),
        fuel = emptyList(),
        accessorials = emptyList(),
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "telegram_user_brokers"
    private const val KEY = "templates"
    private const val MAX = 20
}
