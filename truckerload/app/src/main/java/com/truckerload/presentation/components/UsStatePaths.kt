package com.truckerload.presentation.components

enum class USHeatLevel { HIGH, MEDIUM, LOW }

/** Рейтинг штата: хорошо / плохо / нейтрально / нет данных */
enum class StateRating { GOOD, BAD, NEUTRAL, NO_DATA }

data class USStateMetric(
    val code: String,
    val revenue: Double,
    val trips: Int,
    val level: USHeatLevel,
    val revenuePerMile: Double,
    val avgMilesPerTrip: Double,
    val rating: StateRating
)

/** Известные коды штатов для отчётов (соответствуют GeoJSON). */
fun getUsStateCodes(): Set<String> = setOf(
    "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI",
    "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN",
    "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH",
    "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA",
    "WV", "WI", "WY", "PR"
)

/** Полное название штата для инфо-панели. */
fun getStateDisplayName(code: String): String = when (code) {
    "WA" -> "Вашингтон"
    "CA" -> "Калифорния"
    "OR" -> "Орегон"
    "ID" -> "Айдахо"
    "MT" -> "Монтана"
    "NV" -> "Невада"
    "AZ" -> "Аризона"
    "UT" -> "Юта"
    "WY" -> "Вайоминг"
    "NM" -> "Нью-Мексико"
    "CO" -> "Колорадо"
    "OK" -> "Оклахома"
    "KS" -> "Канзас"
    "NE" -> "Небраска"
    "SD" -> "Южная Дакота"
    "ND" -> "Северная Дакота"
    "MN" -> "Миннесота"
    "WI" -> "Висконсин"
    "IA" -> "Айова"
    "MO" -> "Миссури"
    "TX" -> "Техас"
    "AR" -> "Арканзас"
    "LA" -> "Луизиана"
    "MS" -> "Миссисипи"
    "TN" -> "Теннесси"
    "KY" -> "Кентукки"
    "IL" -> "Иллинойс"
    "IN" -> "Индиана"
    "OH" -> "Огайо"
    "MI" -> "Мичиган"
    "PA" -> "Пенсильвания"
    "NY" -> "Нью-Йорк"
    "NJ" -> "Нью-Джерси"
    "WV" -> "Западная Виргиния"
    "MD" -> "Мэриленд"
    "DE" -> "Делавэр"
    "DC" -> "Вашингтон (окр.)"
    "CT" -> "Коннектикут"
    "RI" -> "Род-Айленд"
    "MA" -> "Массачусетс"
    "VT" -> "Вермонт"
    "NH" -> "Нью-Гэмпшир"
    "ME" -> "Мэн"
    "AL" -> "Алабама"
    "GA" -> "Джорджия"
    "FL" -> "Флорида"
    "SC" -> "Южная Каролина"
    "NC" -> "Северная Каролина"
    "VA" -> "Виргиния"
    else -> code
}
