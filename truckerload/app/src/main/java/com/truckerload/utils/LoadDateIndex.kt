package com.truckerload.utils

import com.truckerload.domain.model.Load

/**
 * Индекс грузов по датам. O(1) поиск вместо O(n).
 * Ключ: YYYY-MM-DD, значение: грузы с этой датой в журнале.
 * Пересчитывается только при изменении массива loads.
 */
object LoadDateIndex {

    /**
     * Строит Map: дата -> список грузов.
     *
     * Календарные маркеры («точки») ставятся только на точную дату груза
     * ([Load.date] / PU), а не на каждый день многодневного рейса — иначе
     * точки выглядят случайными и «заезжают» в будущие месяцы.
     */
    fun build(loads: List<Load>): Map<String, List<Load>> {
        val index = mutableMapOf<String, MutableList<Load>>()
        for (load in loads) {
            val date = exactLoadDate(load) ?: continue
            index.getOrPut(date) { mutableListOf() }.add(load)
        }
        return index
    }

    /** Точная дата груза для маркера календаря: load.date, иначе дата PU. */
    fun exactLoadDate(load: Load): String? {
        canonicalDateString(load.date)?.let { return it }
        return getPickUpDate(load)?.let { canonicalDateString(it) }
    }
}
