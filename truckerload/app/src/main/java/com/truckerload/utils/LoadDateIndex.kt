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
     * Используется для фильтрации «груз активен в этот день».
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

    /**
     * Даты для маркеров (точек) в календаре грузов.
     * Только точная дата груза ([Load.date]), без заполнения дней между PU и DEL —
     * иначе точки «размазываются» по будущим/промежуточным дням и выглядят случайными.
     */
    fun markerDates(loads: List<Load>): Set<String> =
        loads.mapNotNull { canonicalDateString(it.date) }.toSet()
}
