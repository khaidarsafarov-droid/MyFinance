package com.truckerload.utils

import com.truckerload.domain.model.Load

/**
 * Индекс грузов по датам. O(1) поиск вместо O(n).
 * Ключ: YYYY-MM-DD, значение: грузы, активные в эту дату.
 * Пересчитывается только при изменении массива loads.
 */
object LoadDateIndex {

    /**
     * Строит Map: дата -> список грузов.
     * Multi-day loads индексируются по каждой дате в диапазоне (load_date + stops).
     */
    fun build(loads: List<Load>): Map<String, List<Load>> {
        val index = mutableMapOf<String, MutableList<Load>>()
        for (load in loads) {
            val dates = getLoadDateRange(load)
            for (date in dates) {
                index.getOrPut(date) { mutableListOf() }.add(load)
            }
        }
        return index
    }
}
