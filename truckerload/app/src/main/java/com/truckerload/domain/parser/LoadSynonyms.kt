package com.truckerload.domain.parser

/**
 * Label vocabulary shared by the chat parser: English plus the short slang dispatchers use in
 * Russian, Spanish and Uzbek. Words are lowercase; matching is case-insensitive and Unicode-aware.
 */
internal object LoadSynonyms {
    val pickup = listOf(
        "pu", "p/u", "pick ?ups?", "picks?", "origin", "loading",
        "загрузка", "пикап", "берем", "берём", "carga", "recoge", "sale de", "yuklash", "oladi",
    )
    val delivery = listOf(
        "del", "drops?", "deliver(?:y|ies)", "destination", "stop\\s*[2-9]",
        "выгрузка", "деливери", "сдаем", "сдаём", "descarga", "entrega", "llega", "tushirish", "topshiradi",
    )
    val rate = listOf(
        "rate", "total", "payout", "all[\\s-]?in", "price", "рейт", "ставка", "платят", "баксов",
        "pago", "tarifa", "todo incluido", "narxi", "stavka", "jami",
    )
    val deadline = listOf("до", "before", "antes de(?: las)?", "gacha")
    val penalty = listOf(
        "штраф", "multa", "descuento", "shtraf", "jarima", "fine[ds]?", "penalty", "lumper", "лампар", "лампер",
    )
    val deadhead = listOf("deadhead", "empty", "пустой", "vac[ií]o", "bo'sh")
}
