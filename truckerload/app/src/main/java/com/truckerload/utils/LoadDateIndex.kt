package com.truckerload.utils

import com.truckerload.domain.model.Load

/**
 * Индекс грузов по датам. O(1) поиск вместо O(n).
 * Ключ: YYYY-MM-DD, значение: грузы на эту дату.
 * Пересчитывается только при изменении массива loads.
 */
object LoadDateIndex {

    /**
     * Строит Map: дата → список грузов, активных в эту дату (PU…DEL).
     * Используется для фильтров дня/месяца, не для маркеров календаря.
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
     * Даты для зелёных точек в календаре — только точная дата груза (`load.date`),
     * без заполнения всех дней поездки (иначе точки выглядят «рандомно»).
     */
    fun markerDates(loads: List<Load>): Set<String> {
        val dates = mutableSetOf<String>()
        for (load in loads) {
            canonicalDateString(load.date)?.let { dates.add(it) }
        }
        return dates
    }
}
