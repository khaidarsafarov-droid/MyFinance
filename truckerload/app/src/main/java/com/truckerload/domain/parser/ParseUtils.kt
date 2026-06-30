package com.truckerload.domain.parser

import java.util.Calendar
import java.util.Locale

internal object ParseUtils {

    fun parseMoney(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val cleaned = raw.trim()
            .replace("$", "")
            .replace(",", "")
            .replace(" ", "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    fun parseMiles(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val cleaned = raw.trim()
            .replace("mi", "", ignoreCase = true)
            .replace(",", "")
            .trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    fun normalizeDate(raw: String?, defaultYear: Int = Calendar.getInstance().get(Calendar.YEAR)): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        val iso = Regex("""(\d{4})-(\d{2})-(\d{2})""")
        iso.find(trimmed)?.let { return it.value }

        val eu = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
        eu.find(trimmed)?.let {
            val (d, m, y) = it.destructured
            return "$y-$m-$d"
        }

        val us = Regex("""(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?""")
        us.find(trimmed)?.let {
            val month = it.groupValues[1].padStart(2, '0')
            val day = it.groupValues[2].padStart(2, '0')
            val year = when {
                it.groupValues[3].length == 4 -> it.groupValues[3].toIntOrNull() ?: defaultYear
                it.groupValues[3].length == 2 -> 2000 + (it.groupValues[3].toIntOrNull() ?: (defaultYear % 100))
                else -> defaultYear
            }
            return "$year-$month-$day"
        }
        return ""
    }

    fun weekAndYearFromDate(dateStr: String): Pair<Int, Int> {
        if (dateStr.length < 10) {
            val cal = Calendar.getInstance()
            return Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
        }
        return try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
            val y = parts[0].toIntOrNull() ?: return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
            val m = parts[1].toIntOrNull()?.minus(1) ?: return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
            val d = parts[2].toIntOrNull() ?: return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
            val cal = Calendar.getInstance()
            cal.set(y, m, d)
            Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
        } catch (_: Exception) {
            Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
        }
    }

    data class AddressParts(
        val facilityCode: String?,
        val fullAddress: String,
        val city: String,
        val state: String,
        val zip: String
    )

    fun parseAddressLine(raw: String): AddressParts {
        val line = raw.trim().trimEnd(',', ';')
        if (line.isBlank()) {
            return AddressParts(null, "", "", "", "")
        }

        val slashParts = line.split("/").map { it.trim() }.filter { it.isNotBlank() }
        if (slashParts.size >= 2) {
            val facility = slashParts.first().takeIf { it.length <= 24 }
            val street = slashParts.getOrNull(1).orEmpty()
            val cityStateZip = slashParts.drop(2).joinToString(", ").ifBlank { slashParts.last() }
            val (city, state, zip) = parseCityStateZip(cityStateZip)
            val full = listOfNotNull(facility, street, cityStateZip).joinToString(", ")
            return AddressParts(facility, full, city, state, zip)
        }

        val (city, state, zip) = parseCityStateZip(line)
        return AddressParts(null, line, city, state, zip)
    }

    private fun parseCityStateZip(part: String): Triple<String, String, String> {
        val csz = Regex("""(.+?),\s*([A-Za-z]{2,})\s*(\d{5}(?:-\d{4})?)?""", RegexOption.IGNORE_CASE)
        val m = csz.find(part)
        if (m != null) {
            val city = m.groupValues[1].trim()
            val stateRaw = m.groupValues[2].trim()
            val state = normalizeUsState(stateRaw)
            return Triple(
                city,
                state,
                m.groupValues.getOrNull(3).orEmpty()
            )
        }
        return Triple(part.trim(), "", "")
    }

    /** Parses multi-line Relay address block (facility, street, city/state/zip). */
    fun parseMultiLineAddress(lines: List<String>): AddressParts {
        val cleaned = lines.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) {
            return AddressParts(null, "", "", "", "")
        }

        val cityStateIndex = cleaned.indexOfLast { line ->
            parseCityStateZip(line).second.isNotBlank() || parseCityStateZip(line).third.isNotBlank()
        }
        val (city, state, zip) = if (cityStateIndex >= 0) {
            parseCityStateZip(cleaned[cityStateIndex])
        } else {
            Triple("", "", "")
        }

        val facility = if (cityStateIndex > 0) cleaned.first() else cleaned.firstOrNull()
        val streetLines = when {
            cityStateIndex > 1 -> cleaned.subList(1, cityStateIndex)
            cityStateIndex == 1 -> listOf(cleaned[0])
            cleaned.size > 1 -> cleaned.drop(1)
            else -> emptyList()
        }
        val street = streetLines.joinToString(", ")
        val full = listOfNotNull(facility, street.takeIf { it.isNotBlank() }, cleaned.getOrNull(cityStateIndex))
            .filter { it.isNotBlank() }
            .joinToString(", ")

        return AddressParts(
            facilityCode = facility?.takeIf { it.length <= 24 && cityStateIndex > 0 },
            fullAddress = full.ifBlank { cleaned.joinToString(", ") },
            city = city,
            state = state,
            zip = zip,
        )
    }

    fun normalizeUsState(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length == 2) return trimmed.uppercase(Locale.US)
        return US_STATE_NAMES[trimmed.lowercase(Locale.US)] ?: trimmed.uppercase(Locale.US)
    }

    private val US_STATE_NAMES = mapOf(
        "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR",
        "california" to "CA", "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE",
        "florida" to "FL", "georgia" to "GA", "hawaii" to "HI", "idaho" to "ID",
        "illinois" to "IL", "indiana" to "IN", "iowa" to "IA", "kansas" to "KS",
        "kentucky" to "KY", "louisiana" to "LA", "maine" to "ME", "maryland" to "MD",
        "massachusetts" to "MA", "michigan" to "MI", "minnesota" to "MN", "mississippi" to "MS",
        "missouri" to "MO", "montana" to "MT", "nebraska" to "NE", "nevada" to "NV",
        "new hampshire" to "NH", "new jersey" to "NJ", "new mexico" to "NM", "new york" to "NY",
        "north carolina" to "NC", "north dakota" to "ND", "ohio" to "OH", "oklahoma" to "OK",
        "oregon" to "OR", "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC",
        "south dakota" to "SD", "tennessee" to "TN", "texas" to "TX", "utah" to "UT",
        "vermont" to "VT", "virginia" to "VA", "washington" to "WA", "west virginia" to "WV",
        "wisconsin" to "WI", "wyoming" to "WY", "district of columbia" to "DC",
    )

    fun firstMatch(text: String, patterns: List<Regex>): String? {
        for (pattern in patterns) {
            val value = pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }
}
