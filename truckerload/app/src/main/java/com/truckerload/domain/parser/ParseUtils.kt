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

        // DD-MM-YY or DD-MM-YYYY with dashes/slashes (day-first when day > 12).
        val dashed = Regex("""(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})""")
        dashed.find(trimmed)?.let {
            val a = it.groupValues[1].toIntOrNull() ?: return@let
            val b = it.groupValues[2].toIntOrNull() ?: return@let
            val yearRaw = it.groupValues[3]
            val year = when (yearRaw.length) {
                4 -> yearRaw.toIntOrNull() ?: defaultYear
                2 -> 2000 + (yearRaw.toIntOrNull() ?: (defaultYear % 100))
                else -> defaultYear
            }
            val (month, day) = when {
                a > 12 && b in 1..12 -> b to a // DD/MM
                b > 12 && a in 1..12 -> a to b // MM/DD
                else -> a to b // default US MM/DD
            }
            if (month !in 1..12 || day !in 1..31) return@let
            return "%04d-%02d-%02d".format(year, month, day)
        }

        val us = Regex("""(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?""")
        us.find(trimmed)?.let {
            val monthNum = it.groupValues[1].toIntOrNull() ?: return@let
            val dayNum = it.groupValues[2].toIntOrNull() ?: return@let
            val month = it.groupValues[1].padStart(2, '0')
            val day = it.groupValues[2].padStart(2, '0')
            val year = when {
                it.groupValues[3].length == 4 -> it.groupValues[3].toIntOrNull() ?: defaultYear
                it.groupValues[3].length == 2 -> 2000 + (it.groupValues[3].toIntOrNull() ?: (defaultYear % 100))
                else -> com.truckerload.utils.LoadDateRepair.resolveRelayYear(monthNum, dayNum, defaultYear)
            }
            return "$year-$month-$day"
        }

        normalizeTextMonthDate(trimmed, defaultYear).takeIf { it.isNotBlank() }?.let { return it }
        return ""
    }

    /** Parses `Jul 15, 2026` / `July 15 2026` / `15 Jul 2026`. */
    fun normalizeTextMonthDate(
        raw: String?,
        defaultYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    ): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        val monthMap = mapOf(
            "jan" to 1, "january" to 1,
            "feb" to 2, "february" to 2,
            "mar" to 3, "march" to 3,
            "apr" to 4, "april" to 4,
            "may" to 5,
            "jun" to 6, "june" to 6,
            "jul" to 7, "july" to 7,
            "aug" to 8, "august" to 8,
            "sep" to 9, "sept" to 9, "september" to 9,
            "oct" to 10, "october" to 10,
            "nov" to 11, "november" to 11,
            "dec" to 12, "december" to 12,
        )

        val mdY = Regex(
            """(?i)\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\.?\s+(\d{1,2})(?:,)?\s+(\d{2,4})\b""",
        ).find(trimmed)
        if (mdY != null) {
            val month = monthMap[mdY.groupValues[1].lowercase(Locale.US)] ?: return ""
            val day = mdY.groupValues[2].toIntOrNull() ?: return ""
            val yearRaw = mdY.groupValues[3]
            val year = if (yearRaw.length == 2) 2000 + yearRaw.toInt() else yearRaw.toInt()
            if (day !in 1..31) return ""
            return "%04d-%02d-%02d".format(year, month, day)
        }

        val dMY = Regex(
            """(?i)\b(\d{1,2})\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\.?\s+(\d{2,4})\b""",
        ).find(trimmed)
        if (dMY != null) {
            val day = dMY.groupValues[1].toIntOrNull() ?: return ""
            val month = monthMap[dMY.groupValues[2].lowercase(Locale.US)] ?: return ""
            val yearRaw = dMY.groupValues[3]
            val year = if (yearRaw.length == 2) 2000 + yearRaw.toInt() else yearRaw.toInt()
            if (day !in 1..31) return ""
            return "%04d-%02d-%02d".format(year, month, day)
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
        } catch (e: Exception) {
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

        // Relay one-liner: "SWF2, Garner, NC" / "TOL3, Perrysburg, OH 43551"
        parseFacilityCityState(line)?.let { return it }

        val (city, state, zip) = parseCityStateZip(line)
        return AddressParts(null, line, city, state, zip)
    }

    /**
     * `FACILITY, City, ST[ ZIP]` where facility is a Relay warehouse code (no spaces).
     * Requires at least two commas so plain `City, ST` stays a city/state pair.
     */
    private fun parseFacilityCityState(line: String): AddressParts? {
        val m = Regex(
            """^([A-Za-z0-9][A-Za-z0-9\-]{0,23}),\s*(.+),\s*([A-Za-z]{2}|[A-Za-z][A-Za-z .]+[A-Za-z])\s*(\d{5}(?:-\d{4})?)?\s*$""",
        ).find(line) ?: return null
        val facility = m.groupValues[1].trim()
        val city = m.groupValues[2].trim()
        val stateRaw = m.groupValues[3].trim()
        val state = normalizeUsState(stateRaw)
        val zip = m.groupValues.getOrNull(4).orEmpty()
        if (city.isBlank() || state.isBlank()) return null
        // Reject accidental "City, County, ST" where first token looks like a multi-word city fragment.
        if (facility.contains(' ')) return null
        val looksLikeState = state.length == 2 || US_STATE_NAMES.containsKey(stateRaw.lowercase(Locale.US))
        if (!looksLikeState) return null
        return AddressParts(
            facilityCode = facility,
            fullAddress = line,
            city = city,
            state = state,
            zip = zip,
        )
    }

    private fun parseCityStateZip(part: String): Triple<String, String, String> {
        val trimmed = part.trim()
        if (trimmed.isBlank()) return Triple("", "", "")

        // Prefer trailing 2-letter state so "SWF2, Garner, NC" does not treat Garner as state.
        Regex("""^(.+),\s*([A-Za-z]{2})\s+(\d{5}(?:-\d{4})?)\s*$""").find(trimmed)?.let { m ->
            return Triple(
                cityFromLeft(m.groupValues[1]),
                m.groupValues[2].uppercase(Locale.US),
                m.groupValues[3],
            )
        }
        Regex("""^(.+),\s*([A-Za-z]{2})\s*$""").find(trimmed)?.let { m ->
            return Triple(
                cityFromLeft(m.groupValues[1]),
                m.groupValues[2].uppercase(Locale.US),
                "",
            )
        }
        // Full state name: "PERRYSBURG, Ohio 43551"
        Regex("""^(.+),\s*([A-Za-z][A-Za-z .]+[A-Za-z])\s*(\d{5}(?:-\d{4})?)?\s*$""").find(trimmed)?.let { m ->
            val stateRaw = m.groupValues[2].trim()
            val state = normalizeUsState(stateRaw)
            if (state.length == 2 || US_STATE_NAMES.containsKey(stateRaw.lowercase(Locale.US))) {
                return Triple(
                    cityFromLeft(m.groupValues[1]),
                    state,
                    m.groupValues.getOrNull(3).orEmpty(),
                )
            }
        }
        return Triple(trimmed, "", "")
    }

    private fun cityFromLeft(left: String): String {
        val trimmed = left.trim()
        return trimmed.substringAfterLast(',').trim().ifBlank { trimmed }
    }

    /** Parses multi-line Relay address block (facility, street, city/state/zip). */
    fun parseMultiLineAddress(lines: List<String>): AddressParts {
        val cleaned = lines.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) {
            return AddressParts(null, "", "", "", "")
        }
        // One-liner Relay labels: "SWF2, Garner, NC"
        if (cleaned.size == 1) return parseAddressLine(cleaned[0])

        val cityStateIndex = cleaned.indexOfLast { line ->
            val parsed = parseCityStateZip(line)
            parsed.second.isNotBlank() || parsed.third.isNotBlank()
        }
        val (city, state, zip) = if (cityStateIndex >= 0) {
            parseCityStateZip(cleaned[cityStateIndex])
        } else {
            Triple("", "", "")
        }

        val facility = if (cityStateIndex > 0) cleaned.first() else cleaned.firstOrNull()
        val streetLines = when {
            cityStateIndex > 1 -> cleaned.subList(1, cityStateIndex)
            cityStateIndex == 1 -> emptyList()
            cleaned.size > 1 -> cleaned.drop(1)
            else -> emptyList()
        }
        val street = streetLines.joinToString(", ")
        val full = listOfNotNull(facility, street.takeIf { it.isNotBlank() }, cleaned.getOrNull(cityStateIndex))
            .filter { it.isNotBlank() }
            .joinToString(", ")

        return AddressParts(
            facilityCode = facility?.takeIf { it.length <= 24 && !it.contains(',') && cityStateIndex > 0 },
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
