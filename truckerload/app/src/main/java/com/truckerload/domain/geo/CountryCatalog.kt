package com.truckerload.domain.geo

/**
 * ISO country + dialing code for worldwide signup / phone verification.
 * [iso2] is stored in [com.truckerload.data.local.entities.DriverProfileEntity.homeState].
 */
data class CountryInfo(
    val iso2: String,
    val dialCode: String,
    val nameEn: String,
    val nameRu: String,
) {
    fun displayName(isRussian: Boolean): String = if (isRussian) nameRu else nameEn
    val dialLabel: String get() = "$iso2 +$dialCode"
}

object CountryCatalog {
    val countries: List<CountryInfo> = listOf(
        CountryInfo("US", "1", "United States", "США"),
        CountryInfo("CA", "1", "Canada", "Канада"),
        CountryInfo("MX", "52", "Mexico", "Мексика"),
        CountryInfo("GB", "44", "United Kingdom", "Великобритания"),
        CountryInfo("DE", "49", "Germany", "Германия"),
        CountryInfo("FR", "33", "France", "Франция"),
        CountryInfo("ES", "34", "Spain", "Испания"),
        CountryInfo("IT", "39", "Italy", "Италия"),
        CountryInfo("PL", "48", "Poland", "Польша"),
        CountryInfo("UA", "380", "Ukraine", "Украина"),
        CountryInfo("RU", "7", "Russia", "Россия"),
        CountryInfo("KZ", "7", "Kazakhstan", "Казахстан"),
        CountryInfo("UZ", "998", "Uzbekistan", "Узбекистан"),
        CountryInfo("TJ", "992", "Tajikistan", "Таджикистан"),
        CountryInfo("KG", "996", "Kyrgyzstan", "Кыргызстан"),
        CountryInfo("TR", "90", "Turkey", "Турция"),
        CountryInfo("AE", "971", "United Arab Emirates", "ОАЭ"),
        CountryInfo("IN", "91", "India", "Индия"),
        CountryInfo("PK", "92", "Pakistan", "Пакистан"),
        CountryInfo("CN", "86", "China", "Китай"),
        CountryInfo("JP", "81", "Japan", "Япония"),
        CountryInfo("KR", "82", "South Korea", "Южная Корея"),
        CountryInfo("AU", "61", "Australia", "Австралия"),
        CountryInfo("NZ", "64", "New Zealand", "Новая Зеландия"),
        CountryInfo("BR", "55", "Brazil", "Бразилия"),
        CountryInfo("AR", "54", "Argentina", "Аргентина"),
        CountryInfo("ZA", "27", "South Africa", "ЮАР"),
        CountryInfo("NG", "234", "Nigeria", "Нигерия"),
        CountryInfo("EG", "20", "Egypt", "Египет"),
        CountryInfo("IL", "972", "Israel", "Израиль"),
        CountryInfo("SA", "966", "Saudi Arabia", "Саудовская Аравия"),
        CountryInfo("SE", "46", "Sweden", "Швеция"),
        CountryInfo("NO", "47", "Norway", "Норвегия"),
        CountryInfo("NL", "31", "Netherlands", "Нидерланды"),
        CountryInfo("BE", "32", "Belgium", "Бельгия"),
        CountryInfo("CH", "41", "Switzerland", "Швейцария"),
        CountryInfo("AT", "43", "Austria", "Австрия"),
        CountryInfo("PT", "351", "Portugal", "Португалия"),
        CountryInfo("RO", "40", "Romania", "Румыния"),
        CountryInfo("CZ", "420", "Czechia", "Чехия"),
        CountryInfo("HU", "36", "Hungary", "Венгрия"),
        CountryInfo("LT", "370", "Lithuania", "Литва"),
        CountryInfo("LV", "371", "Latvia", "Латвия"),
        CountryInfo("EE", "372", "Estonia", "Эстония"),
        CountryInfo("GE", "995", "Georgia", "Грузия"),
        CountryInfo("AM", "374", "Armenia", "Армения"),
        CountryInfo("AZ", "994", "Azerbaijan", "Азербайджан"),
        CountryInfo("BY", "375", "Belarus", "Беларусь"),
        CountryInfo("MD", "373", "Moldova", "Молдова"),
        CountryInfo("PH", "63", "Philippines", "Филиппины"),
        CountryInfo("VN", "84", "Vietnam", "Вьетнам"),
        CountryInfo("TH", "66", "Thailand", "Таиланд"),
        CountryInfo("ID", "62", "Indonesia", "Индонезия"),
        CountryInfo("MY", "60", "Malaysia", "Малайзия"),
        CountryInfo("SG", "65", "Singapore", "Сингапур"),
        CountryInfo("CO", "57", "Colombia", "Колумбия"),
        CountryInfo("CL", "56", "Chile", "Чили"),
        CountryInfo("PE", "51", "Peru", "Перу"),
    ).sortedBy { it.nameEn }

    val default: CountryInfo = countries.first { it.iso2 == "US" }

    fun byIso2(iso2: String?): CountryInfo? =
        countries.firstOrNull { it.iso2.equals(iso2?.trim(), ignoreCase = true) }

    fun byDialCode(dial: String): CountryInfo? {
        val digits = dial.filter { it.isDigit() }
        return countries
            .filter { it.dialCode == digits }
            .minByOrNull { it.nameEn.length }
    }

    /** Split stored E.164 / loose phone into dial country + national number. */
    fun parsePhone(stored: String?): Pair<CountryInfo, String> {
        if (stored.isNullOrBlank()) return default to ""
        val digits = stored.filter { it.isDigit() }
        if (digits.isEmpty()) return default to ""
        val match = countries
            .sortedByDescending { it.dialCode.length }
            .firstOrNull { digits.startsWith(it.dialCode) }
        return if (match != null) {
            match to digits.removePrefix(match.dialCode)
        } else {
            default to digits
        }
    }

    fun formatE164(country: CountryInfo, nationalNumber: String): String {
        val national = nationalNumber.filter { it.isDigit() }
        return "+${country.dialCode}$national"
    }
}
