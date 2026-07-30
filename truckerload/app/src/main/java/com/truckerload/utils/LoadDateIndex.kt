package com.truckerload.utils

import com.truckerload.domain.model.Load

/**
 * Индекс грузов по датам. O(1) поиск вместо O(n).
 * Ключ: YYYY-MM-DD, значение: грузы с этой датой в журнале.
 * Пересчитывается только при изменении массива loads.
 *
 * Calendar dots use the load's journal date ([Load.date]) only — not the full
 * PU→DEL active span — so markers sit on the exact load day.
 */
object LoadDateIndex {

    /**
     * Строит Map: дата журнала (YYYY-MM-DD) -> список грузов.
     * Одна точка на груз: [Load.date], без заполнения промежуточных дней рейса.
     */
    fun build(loads: List<Load>): Map<String, List<Load>> {
        val index = mutableMapOf<String, MutableList<Load>>()
        for (load in loads) {
            val date = canonicalDateString(load.date) ?: continue
            index.getOrPut(date) { mutableListOf() }.add(load)
        }
        return index
    }

    /** Dates that should show a calendar marker (exact journal dates). */
    fun markerDates(loads: List<Load>): Set<String> = build(loads).keys
}
