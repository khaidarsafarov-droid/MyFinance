package com.truckerload.utils

import com.truckerload.domain.model.Load

/**
 * Индекс грузов по датам для календаря. O(1) поиск вместо O(n).
 * Ключ: YYYY-MM-DD, значение: грузы с событием в эту дату (PU/DEL/load.date).
 * Пересчитывается только при изменении массива loads.
 */
object LoadDateIndex {

    /**
     * Строит Map: дата -> список грузов.
     * Маркеры — только точные даты событий, без заполнения всех дней в пути.
     */
    fun build(loads: List<Load>): Map<String, List<Load>> {
        val index = mutableMapOf<String, MutableList<Load>>()
        for (load in loads) {
            val dates = getLoadMarkerDates(load)
            for (date in dates) {
                index.getOrPut(date) { mutableListOf() }.add(load)
            }
        }
        return index
    }
}
