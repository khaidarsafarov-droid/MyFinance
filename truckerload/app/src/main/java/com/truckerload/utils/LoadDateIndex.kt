package com.truckerload.utils

import com.truckerload.domain.model.Load

/**
 * Индекс грузов по датам. O(1) поиск вместо O(n).
 * Ключ: YYYY-MM-DD, значение: грузы с этой датой журнала.
 * Пересчитывается только при изменении массива loads.
 *
 * Calendar dots use the exact journal date ([Load.date]), not the full
 * PU→DEL active range — otherwise multi-day trips paint every day in between.
 */
object LoadDateIndex {

    /**
     * Строит Map: дата журнала -> список грузов.
     * Один маркер на груз: каноническая [Load.date] (дата PU / записи).
     */
    fun build(loads: List<Load>): Map<String, List<Load>> {
        val index = mutableMapOf<String, MutableList<Load>>()
        for (load in loads) {
            val date = markerDate(load) ?: continue
            index.getOrPut(date) { mutableListOf() }.add(load)
        }
        return index
    }

    /** Exact calendar-dot date for a load, or null if unusable. */
    fun markerDate(load: Load): String? = canonicalDateString(load.date)
}
