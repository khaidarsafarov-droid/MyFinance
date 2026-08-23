package com.truckerload.voice

import com.truckerload.domain.assistant.AssistantToolCall
import com.truckerload.presentation.navigation.Routes

/**
 * Maps Google Assistant deep links / BII extras onto journal actions.
 * Does not log parameter values.
 */
object AppVoiceJournal {
    const val PATH_ADD_DIESEL = "add_diesel"
    const val PATH_ADD_PAYCHECK = "add_paycheck"
    const val PATH_WEEKLY_GROSS = "weekly_gross"
    const val PATH_SEARCH = "search"

    fun actionFromPath(path: String, query: Map<String, String>): AppVoiceAction? {
        return when (path.lowercase()) {
            PATH_ADD_DIESEL, "diesel" -> addDiesel(query)
            PATH_ADD_PAYCHECK, "paycheck" -> addPaycheck(query)
            PATH_WEEKLY_GROSS, "gross" -> queryWeeklyGross(query)
            PATH_SEARCH, "get_thing" -> fromSearch(query)
            else -> null
        }
    }

    fun fromSearch(query: Map<String, String>): AppVoiceAction {
        val spoken = query.firstOf("q", "query", "thing.name", "name", "feature", "featurename")
            .orEmpty()
        return fromSpokenQuery(spoken) ?: AppVoiceAction.OpenScreen(Routes.HOME)
    }

    fun fromSpokenQuery(spoken: String): AppVoiceAction? {
        val key = AppVoiceActions.normalize(spoken)
        if (key.isBlank()) return null
        if (isWeeklyGrossQuery(key) || key == PATH_WEEKLY_GROSS || key == "gross") {
            return AppVoiceAction.QueryWeeklyGross(
                weekNumber = parseSpokenWeekNumber(key),
                year = parseSpokenYear(key),
            )
        }
        if (key == PATH_ADD_DIESEL) {
            return AppVoiceAction.AddDiesel(amount = null, gallons = null, date = null)
        }
        if (key == PATH_ADD_PAYCHECK) {
            return AppVoiceAction.AddPaycheck(amount = null, weekNumber = null, year = null)
        }
        return null
    }

    fun addDiesel(query: Map<String, String>): AppVoiceAction.AddDiesel {
        return AppVoiceAction.AddDiesel(
            amount = parseAmount(query.firstOf("amount", "total", "price")),
            gallons = parseAmount(query.firstOf("gallons", "gallon", "gal")),
            date = query.firstOf("date"),
        )
    }

    fun addPaycheck(query: Map<String, String>): AppVoiceAction.AddPaycheck {
        return AppVoiceAction.AddPaycheck(
            amount = parseAmount(query.firstOf("amount", "net", "paycheck")),
            weekNumber = parseInt(query.firstOf("weeknumber", "week", "week_number")),
            year = parseInt(query.firstOf("year")),
        )
    }

    fun queryWeeklyGross(query: Map<String, String>): AppVoiceAction.QueryWeeklyGross {
        return AppVoiceAction.QueryWeeklyGross(
            weekNumber = parseInt(query.firstOf("weeknumber", "week", "week_number")),
            year = parseInt(query.firstOf("year")),
        )
    }

    fun toToolCall(action: AppVoiceAction): AssistantToolCall? {
        return when (action) {
            is AppVoiceAction.AddDiesel -> {
                val amount = action.amount ?: return null
                AssistantToolCall.AddDiesel(amount, action.gallons, action.date)
            }
            is AppVoiceAction.AddPaycheck -> {
                val amount = action.amount ?: return null
                AssistantToolCall.AddPaycheck(amount, action.weekNumber, action.year)
            }
            is AppVoiceAction.QueryWeeklyGross ->
                AssistantToolCall.QueryWeeklyGross(action.weekNumber, action.year)
            else -> null
        }
    }

    fun formRoute(action: AppVoiceAction): String? {
        return when (action) {
            is AppVoiceAction.AddDiesel -> Routes.ADD_DIESEL
            is AppVoiceAction.AddPaycheck -> Routes.ADD_PAYCHECK
            is AppVoiceAction.QueryWeeklyGross -> Routes.STATS
            else -> null
        }
    }

    fun isWeeklyGrossQuery(normalized: String): Boolean {
        if (normalized.contains("гросс") || normalized.contains("gross")) return true
        if (normalized.contains("weekly revenue") || normalized.contains("week revenue")) return true
        if (normalized.contains("итого за неделю") || normalized.contains("total this week")) return true
        return false
    }

    fun parseAmount(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
            .replace(',', '.')
            .replace(Regex("[^0-9.\\-]"), "")
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite() || value <= 0.0) return null
        return value
    }

    fun parseInt(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val digits = Regex("(-?\\d+)").find(raw.trim())?.groupValues?.getOrNull(1) ?: return null
        return digits.toIntOrNull()
    }

    fun parseSpokenWeekNumber(normalized: String): Int? {
        val match = Regex("""(?:week|недел[яиюе])\s+(\d{1,2})""").find(normalized) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..53 }
    }

    fun parseSpokenYear(normalized: String): Int? {
        val match = Regex("""(?:year|год[ау]?)\s+(\d{4})""").find(normalized) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1970..2100 }
    }

    private fun Map<String, String>.firstOf(vararg keys: String): String? {
        keys.forEach { key ->
            val value = this[key]?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return null
    }
}
